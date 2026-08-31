package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.entity.CommentLike;
import com.blog.entity.CommentReport;
import com.blog.entity.User;
import com.blog.vo.CommentPublicVO;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.CommentReportMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    public static final String GUESTBOOK = "GUESTBOOK";
    public static final String ARTICLE = "ARTICLE";

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final SiteConfigService siteConfigService;
    private final CommentNotificationService notificationService;
    private final CommentLikeMapper commentLikeMapper;
    private final CommentReportMapper commentReportMapper;

    public CommentService(CommentMapper commentMapper, ArticleMapper articleMapper,
                          UserMapper userMapper, SiteConfigService siteConfigService,
                          CommentNotificationService notificationService,
                          CommentLikeMapper commentLikeMapper,
                          CommentReportMapper commentReportMapper) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
        this.siteConfigService = siteConfigService;
        this.notificationService = notificationService;
        this.commentLikeMapper = commentLikeMapper;
        this.commentReportMapper = commentReportMapper;
    }

    /** 评论嵌套深度上限:防止构造深链拖慢 buildTree。 */
    private static final int MAX_PARENT_DEPTH = 5;

    public Comment create(CommentDTO dto, String ip, String ua) {
        boolean guestbook = GUESTBOOK.equals(dto.getTargetType());
        if (!guestbook && !ARTICLE.equals(dto.getTargetType())) throw new BizException("评论对象类型无效");
        if (guestbook) {
            dto.setArticleId(0L); // No article reference; target_type determines the namespace.
        } else {
            if (dto.getArticleId() == null || dto.getArticleId() <= 0) throw new BizException("文章 id 必填");
            var article = articleMapper.selectById(dto.getArticleId());
            ArticleVisibility.requirePublic(article);
            if (!Integer.valueOf(1).equals(article.getAllowComment())) throw new BizException("文章已关闭评论");
        }
        // 校验 parentId:必须属于同一文章,且嵌套深度不超过 MAX_PARENT_DEPTH
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BizException("父评论不存在");
            }
            if (!Objects.equals(parent.getArticleId(), dto.getArticleId())
                    || !Objects.equals(parent.getTargetType(), dto.getTargetType())
                    || !Integer.valueOf(1).equals(parent.getStatus())) {
                throw new BizException("父评论不属于同一文章");
            }
            // 沿 parentId 链向上数深度,超限拒绝
            int depth = 1;
            Long cursor = parent.getParentId();
            while (cursor != null && cursor > 0 && depth < MAX_PARENT_DEPTH) {
                Comment p = commentMapper.selectById(cursor);
                if (p == null) break;
                cursor = p.getParentId();
                depth++;
            }
            if (depth >= MAX_PARENT_DEPTH) {
                throw new BizException("评论嵌套层级过深");
            }
        }
        String audit = siteConfigService.get("comment_audit", "0");
        Comment c = new Comment();
        c.setArticleId(dto.getArticleId());
        c.setTargetType(dto.getTargetType());
        c.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        c.setNickname(sanitize(dto.getNickname()));
        c.setEmail(dto.getEmail());
        c.setWebsite(dto.getWebsite());
        c.setContent(maskSensitive(clean(dto.getContent())));
        // 优先使用用户上传的头像；否则留空,前端首字母 fallback 立刻接管
        // (之前用 Gravatar 兜底,国内访问慢,导致 name 头像迟迟不显示)
        if (dto.getAvatar() != null && !dto.getAvatar().trim().isEmpty()) {
            c.setAvatar(dto.getAvatar().trim());
        } else {
            c.setAvatar("");
        }
        c.setIp(ip);
        c.setUa(ua == null ? "" : ua);
        c.setContentType(dto.getContentType() == null ? 0 : dto.getContentType());
        c.setStatus("1".equals(audit) ? 0 : 1);
        c.setCreateTime(LocalDateTime.now());
        commentMapper.insert(c);
        // 新评论提交即通知管理员(不等审核通过),让他知道有待审
        // 异步 + 内部 try/catch,不影响评论提交响应
        notificationService.onCreated(c);
        return c;
    }

    /**
     * 点赞 / 取消点赞。按 IP(游客)或登录用户去重,同一人同一留言只能点一次。
     * 再次调用即取消点赞。返回最新 {liked, likeCount}。
     */
    @Transactional
    public Map<String, Object> like(Long commentId, String ip, Long userId) {
        Comment c = commentMapper.lockById(commentId);
        if (c == null || c.getStatus() == null || c.getStatus() != 1) {
            throw new BizException("评论不存在或待审核");
        }
        requireVisible(c);
        LambdaQueryWrapper<CommentLike> w = new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId);
        if (userId != null) {
            w.eq(CommentLike::getUserId, userId);
        } else {
            w.isNull(CommentLike::getUserId).eq(CommentLike::getIp, ip == null ? "" : ip);
        }
        CommentLike existing = commentLikeMapper.selectOne(w);
        boolean liked;
        if (existing != null) {
            commentLikeMapper.deleteById(existing.getId());
            c.setLikeCount(Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) - 1));
            liked = false;
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setIp(userId == null ? (ip == null ? "" : ip) : "user:" + userId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(like);
            c.setLikeCount((c.getLikeCount() == null ? 0 : c.getLikeCount()) + 1);
            liked = true;
        }
        commentMapper.adjustLikes(commentId, liked ? 1 : -1);
        Map<String, Object> m = new HashMap<>();
        m.put("liked", liked);
        m.put("likeCount", c.getLikeCount());
        return m;
    }

    /**
     * 举报评论。写入 comment_report 并累加 comment.report_count。
     * 风控:调用方需对 IP 限频,避免被滥用刷举报。
     */
    @Transactional
    public void report(Long commentId, String reason, String detail, String email, String ip) {
        Comment c = commentMapper.lockById(commentId);
        if (c == null || c.getStatus() == null || c.getStatus() != 1) {
            throw new BizException("评论不存在或待审核");
        }
        requireVisible(c);
        CommentReport r = new CommentReport();
        r.setCommentId(commentId);
        r.setReason(reason == null ? "" : reason);
        r.setDetail(detail == null ? "" : detail);
        r.setEmail(email == null ? "" : email);
        r.setIp(ip == null ? "" : ip);
        r.setStatus(0);
        r.setCreateTime(LocalDateTime.now());
        commentReportMapper.insert(r);
        c.setReportCount((c.getReportCount() == null ? 0 : c.getReportCount()) + 1);
        commentMapper.incrementReports(commentId);
    }

    public List<CommentPublicVO> treeByArticle(Long articleId, boolean includePending) {
        return treeByArticle(articleId, includePending, 1, 50);
    }

    public List<CommentPublicVO> treeByArticle(Long articleId, boolean includePending, long page, long size) {
        if (!includePending) ArticleVisibility.requirePublic(articleMapper.selectById(articleId));
        return pagedTree(ARTICLE, articleId, includePending, page, size);
    }

    public List<CommentPublicVO> guestbook() { return guestbook(1, 50); }

    public List<CommentPublicVO> guestbook(long page, long size) {
        return pagedTree(GUESTBOOK, 0L, false, page, size);
    }

    private List<CommentPublicVO> pagedTree(String type, Long articleId, boolean includePending, long page, long size) {
        var rootsQuery = new LambdaQueryWrapper<Comment>().eq(Comment::getTargetType, type)
                .eq(Comment::getArticleId, articleId)
                .and(w -> w.eq(Comment::getParentId, 0).or().apply("NOT EXISTS (SELECT 1 FROM comment parent WHERE parent.id = comment.parent_id AND parent.deleted = 0 AND parent.target_type = comment.target_type AND parent.article_id = comment.article_id" + (includePending ? "" : " AND parent.status = 1") + ")"))
                .eq(!includePending, Comment::getStatus, 1).orderByDesc(Comment::getCreateTime).orderByDesc(Comment::getId);
        List<Comment> roots = commentMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(50, size))), rootsQuery).getRecords();
        List<Comment> all = new ArrayList<>(roots);
        List<Long> parents = roots.stream().map(Comment::getId).toList();
        for (int level = 0; level < MAX_PARENT_DEPTH && !parents.isEmpty() && all.size() < 1000; level++) {
            var children = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getTargetType, type).eq(Comment::getArticleId, articleId)
                    .in(Comment::getParentId, parents).eq(!includePending, Comment::getStatus, 1)
                    .orderByAsc(Comment::getId).last("LIMIT " + (1000 - all.size())));
            all.addAll(children);
            parents = children.stream().map(Comment::getId).toList();
        }
        List<CommentPublicVO> tree = toPublicTree(all);
        markRemainingReplies(tree, all, type, articleId, includePending);
        return tree;
    }

    /** Cursor pagination keeps every reply reachable even when the initial tree reaches its size limit. */
    public Map<String, Object> replies(Long parentId, long afterId, long size) {
        Comment parent = commentMapper.selectById(parentId);
        if (parent == null || !Integer.valueOf(1).equals(parent.getStatus())) throw new BizException(404, "评论不存在");
        requireVisible(parent);
        int limit = (int) Math.max(1, Math.min(50, size));
        List<Comment> rows = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, parent.getTargetType()).eq(Comment::getArticleId, parent.getArticleId())
                .eq(Comment::getParentId, parentId).eq(Comment::getStatus, 1)
                .gt(Comment::getId, Math.max(0, afterId)).orderByAsc(Comment::getId).last("LIMIT " + (limit + 1)));
        boolean hasMore = rows.size() > limit;
        List<Comment> records = rows.subList(0, Math.min(limit, rows.size()));
        User admin = findPublicAdmin();
        List<CommentPublicVO> result = records.stream().map(c -> {
            c.setParentName(parent.getNickname());
            c.setReplies(List.of());
            return toPublicVo(c, admin);
        }).toList();
        markRemainingReplies(result, records, parent.getTargetType(), parent.getArticleId(), false);
        return Map.of("records", result, "hasMore", hasMore,
                "nextCursor", records.isEmpty() ? Math.max(0, afterId) : records.get(records.size() - 1).getId());
    }

    private void markRemainingReplies(List<CommentPublicVO> tree, List<Comment> all, String type, Long articleId, boolean includePending) {
        if (all.isEmpty()) return;
        Map<Long, Long> counts = commentMapper.countReplies(all.stream().map(Comment::getId).toList(), type, articleId, includePending)
                .stream().collect(Collectors.toMap(com.blog.vo.CommentReplyCount::getParentId, com.blog.vo.CommentReplyCount::getCount));
        Deque<CommentPublicVO> pending = new ArrayDeque<>(tree);
        while (!pending.isEmpty()) {
            CommentPublicVO node = pending.removeFirst();
            List<CommentPublicVO> children = node.getReplies() == null ? List.of() : node.getReplies();
            node.setHasMoreReplies(counts.getOrDefault(node.getId(), 0L) > children.size());
            pending.addAll(children);
        }
    }

    private void requireVisible(Comment comment) {
        if (!GUESTBOOK.equals(comment.getTargetType())) ArticleVisibility.requirePublic(articleMapper.selectById(comment.getArticleId()));
    }

    /** 前台提交评论成功后回显也走公开 VO，避免把作者自己的邮箱/IP/UA 回吐给客户端 */
    public CommentPublicVO toPublic(Comment c) { return toPublicVo(c, findPublicAdmin()); }

    public List<Comment> treeAll() {
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreateTime).last("LIMIT 200"));
        Set<Long> ids = all.stream().map(Comment::getArticleId).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            articleMapper.selectBatchIds(ids).forEach(a -> {
                for (Comment c : all) if (c.getArticleId().equals(a.getId())) c.setArticleTitle(a.getTitle());
            });
        }
        return all;
    }

    public long countPending() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 0));
    }

    public void approve(Long id) {
        Comment c = new Comment(); c.setId(id); c.setStatus(1); commentMapper.updateById(c);
        // 审核通过后异步通知作者/被回复者/留言板 admin。
        // 失败不回滚审核(通知是旁路),NotificationService 内部已 try/catch。
        notificationService.onApproved(id);
    }

    public void markSpam(Long id) {
        Comment c = new Comment(); c.setId(id); c.setStatus(2); commentMapper.updateById(c);
    }

    public void delete(Long id) { commentMapper.deleteById(id); }

    /**
     * 管理员切换评论的"精选"状态。同 articleId 同一时刻保持最多一条精选：
     * 置顶某条时,先把同文章其他精选清掉。
     */
    public void setFeatured(Long id, boolean featured) {
        Comment c = commentMapper.selectById(id);
        if (c == null) throw new BizException("评论不存在");
        if (featured) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .set(Comment::getFeatured, 0)
                .eq(Comment::getArticleId, c.getArticleId()).eq(Comment::getTargetType, c.getTargetType())
                .ne(Comment::getId, id));
        }
        Comment u = new Comment();
        u.setId(id);
        u.setFeatured(featured ? 1 : 0);
        commentMapper.updateById(u);
    }

    /**
     * 管理员后台回复评论/留言。复用 parentId 嵌套,直接通过(status=1),
     * 绕过前台 create 的审核开关/限流/深度校验。
     * 身份取当前登录管理员在个人中心设置的昵称/头像/邮箱,未设置时 fallback 到用户名/默认头像。
     */
    public Comment adminReply(Long parentId, String content) {
        if (parentId == null || parentId <= 0) throw new BizException("回复对象不存在");
        Comment parent = commentMapper.selectById(parentId);
        if (parent == null) throw new BizException("回复的评论不存在");
        String text = clean(content);
        if (text == null || text.isBlank()) throw new BizException("回复内容不能为空");

        // 取当前登录管理员资料
        Long adminId = SecurityUtil.require().getId();
        User admin = userMapper.selectById(adminId);
        if (admin == null) throw new BizException("当前管理员账号不存在");
        String nickname = (admin.getNickname() != null && !admin.getNickname().isBlank())
                ? admin.getNickname() : admin.getUsername();
        if (nickname == null || nickname.isBlank()) nickname = "站长";
        String email = admin.getEmail() != null ? admin.getEmail().trim() : "";
        String avatar = admin.getAvatar() != null ? admin.getAvatar().trim() : "";
        if (avatar.isEmpty()) {
            // 不再生成 Gravatar 兜底(国内访问慢导致 name 头像迟迟不显示),前端首字母 fallback 立刻接管
        }

        Comment c = new Comment();
        c.setTargetType(parent.getTargetType());
        c.setArticleId(parent.getArticleId());   // 与父同文章(评论/留言自动一致)
        c.setParentId(parentId);
        c.setNickname(nickname);
        c.setEmail(email);
        c.setContent(text);
        c.setAvatar(avatar);
        c.setIp("");
        c.setStatus(1);                            // 直接通过,绕过 comment_audit 开关
        c.setCreateTime(LocalDateTime.now());
        commentMapper.insert(c);
        // 仅通知被回复者,不给管理员自己发"审核通过"邮件
        notificationService.onAdminReply(c);
        return c;
    }

    /**
     * 管理员修改个人资料时，同步历史后台回复保存的昵称、邮箱和头像快照。
     *
     * <p>后台回复不会接受客户端 IP/UA，而访客评论一定由服务端写入这两个字段；
     * 再叠加修改前的管理员邮箱/昵称，可以避免把普通访客评论误判为后台回复。
     * 该同步不需要新增数据库字段或表。</p>
     */
    public int syncAdminReplyProfile(User previous, User current) {
        if (previous == null || current == null) return 0;

        String oldEmail = normalized(previous.getEmail());
        String oldNickname = normalized(previous.getNickname());
        String username = normalized(previous.getUsername());
        if (oldEmail.isEmpty() && oldNickname.isEmpty() && username.isEmpty()) return 0;

        return commentMapper.syncAdminReplyProfile(
                oldEmail,
                oldNickname,
                username,
                displayName(current),
                normalized(current.getEmail()),
                normalized(current.getAvatar()));
    }

    public long totalApproved() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1));
    }

    public long countApproved() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1));
    }

    public long countSpam() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 2));
    }

    // ── 留言板专用统计 ──
    public long countPendingGuestbook() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, GUESTBOOK).eq(Comment::getStatus, 0));
    }
    public long countApprovedGuestbook() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, GUESTBOOK).eq(Comment::getStatus, 1));
    }
    public long countSpamGuestbook() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, GUESTBOOK).eq(Comment::getStatus, 2));
    }

    public Page<Comment> page(Page<Comment> p,
                          LambdaQueryWrapper<Comment> w) {
        return commentMapper.selectPage(p, w);
    }

    /** 给一批 Comment 填充 parentName（回复对象的昵称） */
    public void fillParentNames(List<Comment> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> parentIds = records.stream()
                .map(Comment::getParentId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (parentIds.isEmpty()) return;
        List<Comment> parents = commentMapper.selectBatchIds(parentIds);
        Map<Long, String> nameMap = new HashMap<>();
        for (Comment p : parents) nameMap.put(p.getId(), p.getNickname());
        for (Comment c : records) {
            if (c.getParentId() != null && c.getParentId() > 0) {
                c.setParentName(nameMap.get(c.getParentId()));
            }
        }
    }

    private List<Comment> buildTree(List<Comment> all) {
        Map<Long, Comment> map = new LinkedHashMap<>();
        for (Comment comment : all) {
            comment.setReplies(new ArrayList<>());
            map.put(comment.getId(), comment);
        }
        List<Comment> roots = new ArrayList<>();
        for (Comment comment : map.values()) {
            Comment parent = map.get(comment.getParentId());
            Set<Long> ancestors = new HashSet<>();
            ancestors.add(comment.getId());
            Comment cursor = parent;
            boolean cycle = false;
            while (cursor != null) {
                if (!ancestors.add(cursor.getId()) || ancestors.size() > MAX_PARENT_DEPTH + 1) { cycle = true; break; }
                cursor = map.get(cursor.getParentId());
            }
            if (parent == null || cycle || !Objects.equals(parent.getArticleId(), comment.getArticleId())
                    || !Objects.equals(parent.getTargetType(), comment.getTargetType())) {
                roots.add(comment);
            } else {
                comment.setParentName(parent.getNickname());
                parent.getReplies().add(comment);
            }
        }
        return roots;
    }

    /** 公开视图：剔除 email/ip/ua 等隐私字段，并递归转换子回复 */
    private List<CommentPublicVO> toPublicTree(List<Comment> all) {
        List<Comment> roots = buildTree(all);
        User admin = findPublicAdmin();
        return roots.stream().map(c -> toPublicVo(c, admin)).collect(Collectors.toList());
    }

    private CommentPublicVO toPublicVo(Comment c, User admin) {
        CommentPublicVO v = new CommentPublicVO();
        v.setId(c.getId());
        v.setArticleId(c.getArticleId());
        v.setParentId(c.getParentId());
        v.setNickname(c.getNickname());
        v.setWebsite(c.getWebsite());
        v.setContent(c.getContent());
        boolean adminReply = isAdminReply(c, admin);
        v.setIsAdmin(adminReply);
        v.setAvatar(adminReply && !normalized(admin.getAvatar()).isEmpty()
                ? normalized(admin.getAvatar()) : c.getAvatar());
        v.setStatus(c.getStatus());
        v.setLikeCount(c.getLikeCount());
        v.setReportCount(c.getReportCount());
        v.setContentType(c.getContentType());
        v.setFeatured(c.getFeatured());
        v.setCreateTime(c.getCreateTime());
        v.setArticleTitle(c.getArticleTitle());
        v.setParentName(c.getParentName());
        if (c.getReplies() != null) {
            v.setReplies(c.getReplies().stream().map(reply -> toPublicVo(reply, admin)).collect(Collectors.toList()));
        }
        return v;
    }

    /** 当前有效管理员仅用于公开昵称/头像覆盖，不会把邮箱等隐私字段写入公开 VO。 */
    private User findPublicAdmin() {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "ADMIN")
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId)
                .last("LIMIT 1"));
    }

    private boolean isAdminReply(Comment c, User admin) {
        if (c == null || admin == null || c.getParentId() == null || c.getParentId() <= 0) return false;
        if (!normalized(c.getIp()).isEmpty() || !normalized(c.getUa()).isEmpty()) return false;

        String commentEmail = normalized(c.getEmail());
        String adminEmail = normalized(admin.getEmail());
        if (!commentEmail.isEmpty() && commentEmail.equalsIgnoreCase(adminEmail)) return true;

        String commentName = normalized(c.getNickname());
        return !commentName.isEmpty()
                && (commentName.equals(displayName(admin)) || commentName.equals(normalized(admin.getUsername())));
    }

    private String displayName(User user) {
        if (user == null) return "站长";
        String nickname = normalized(user.getNickname());
        if (!nickname.isEmpty()) return nickname;
        String username = normalized(user.getUsername());
        return username.isEmpty() ? "站长" : username;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private String clean(String html) {
        if (html == null) return null;
        // 自定义白名单:基础排版 + 代码块。明确不放行 iframe/object/embed/script/
        // form/input/style/on* 属性。注释现在已是 markdown-friendly 的少量 HTML,
        // 渲染端 Vue 用 {{ }} 转义,这里收紧只为防御未来误用 v-html。
        Safelist safe = Safelist.basic()
                .addTags("code", "pre", "span")
                .addAttributes("a", "href", "title")
                .addAttributes("span", "class")
                .addAttributes("code", "class")
                .addAttributes("pre", "class")
                .addProtocols("a", "href", "http", "https", "mailto");
        return Jsoup.clean(html, safe);
    }

    private String sanitize(String s) {
        if (s == null) return null;
        return Jsoup.clean(s, Safelist.none()).trim();
    }

    /** 敏感词掩码:命中站点配置 sensitive_words 的词组替换为 **（软处理,不拒绝提交）。 */
    private String maskSensitive(String text) {
        if (text == null) return null;
        String words = siteConfigService.get("sensitive_words", "");
        if (words == null || words.isBlank()) return text;
        String masked = text;
        for (String w : words.split("[,\\s]+")) {
            if (!w.isEmpty()) masked = masked.replace(w, "**");
        }
        return masked;
    }

}
