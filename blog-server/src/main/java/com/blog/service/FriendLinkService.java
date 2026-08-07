package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.FriendLinkApplyDTO;
import com.blog.dto.FriendLinkDTO;
import com.blog.dto.FriendLinkQuery;
import com.blog.entity.FriendLink;
import com.blog.mapper.FriendLinkMapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class FriendLinkService {
    private final FriendLinkMapper friendLinkMapper;
    private final FriendLinkNotificationService friendLinkNotificationService;

    public FriendLinkService(FriendLinkMapper friendLinkMapper,
                             FriendLinkNotificationService friendLinkNotificationService) {
        this.friendLinkMapper = friendLinkMapper;
        this.friendLinkNotificationService = friendLinkNotificationService;
    }

    public Page<FriendLink> page(FriendLinkQuery q) {
        Page<FriendLink> p = Page.of(q.getPage(), q.getSize());
        LambdaQueryWrapper<FriendLink> w = new LambdaQueryWrapper<>();
        if (q.getStatus() != null) w.eq(FriendLink::getStatus, q.getStatus());
        if (StringUtils.hasText(q.getKeyword())) {
            w.and(z -> z.like(FriendLink::getName, q.getKeyword())
                    .or().like(FriendLink::getDescription, q.getKeyword())
                    .or().like(FriendLink::getUrl, q.getKeyword()));
        }
        w.orderByAsc(FriendLink::getSortOrder).orderByDesc(FriendLink::getId);
        return friendLinkMapper.selectPage(p, w);
    }

    public List<FriendLink> listAll() {
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .orderByAsc(FriendLink::getSortOrder).orderByDesc(FriendLink::getId));
    }

    public List<FriendLink> listPublished() {
        // 推荐的优先排前；同推荐级别内按 sortOrder ASC
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getStatus, 1)
                .orderByDesc(FriendLink::getRecommended)
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getId));
    }

    public FriendLink byId(Long id) {
        FriendLink f = friendLinkMapper.selectById(id);
        if (f == null) throw new BizException(404, "友链不存在");
        return f;
    }

    public FriendLink save(FriendLinkDTO dto) {
        FriendLink f = new FriendLink();
        BeanUtils.copyProperties(dto, f);
        if (f.getStatus() == null) f.setStatus(0);
        if (f.getRecommended() == null) f.setRecommended(0);
        if (f.getSortOrder() == null || f.getSortOrder() == 0) f.setSortOrder(nextSortOrder());
        pushDownSortOrder(null, f.getSortOrder());
        renumberSortOrder();
        friendLinkMapper.insert(f);
        return byId(f.getId());
    }

    /** 前台用户提交友链申请，默认待审核 */
    public FriendLink apply(FriendLinkApplyDTO dto) {
        FriendLink f = new FriendLink();
        BeanUtils.copyProperties(dto, f);
        f.setName(sanitize(f.getName()));
        f.setDescription(sanitize(f.getDescription()));
        f.setStatus(0);
        f.setSortOrder(nextSortOrder());
        pushDownSortOrder(null, f.getSortOrder());
        renumberSortOrder();
        f.setLinkGroup("网友");
        friendLinkMapper.insert(f);
        friendLinkNotificationService.onApplied(f);
        return byId(f.getId());
    }

    private static String sanitize(String s) {
        if (s == null) return null;
        return Jsoup.clean(s, Safelist.none()).trim();
    }

    private int nextSortOrder() {
        Integer max = friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                        .select(FriendLink::getSortOrder))
                .stream().map(FriendLink::getSortOrder).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(-1);
        return max + 1;
    }

    /** 位次冲突下推：sortOrder>=target 的其他记录降序 +1 */
    private void pushDownSortOrder(Long excludeId, int target) {
        LambdaQueryWrapper<FriendLink> w = new LambdaQueryWrapper<FriendLink>()
                .ne(excludeId != null, FriendLink::getId, excludeId)
                .ge(FriendLink::getSortOrder, target)
                .orderByDesc(FriendLink::getSortOrder);
        List<FriendLink> conflicts = friendLinkMapper.selectList(w);
        for (FriendLink c : conflicts) {
            c.setSortOrder(c.getSortOrder() + 1);
            friendLinkMapper.updateById(c);
        }
    }

    /** 归一化位次：按当前排序重排为 1,2,3...（消除 pushDown 留下的空位） */
    private void renumberSortOrder() {
        List<FriendLink> all = friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getId));
        for (int i = 0; i < all.size(); i++) {
            int target = i + 1;
            FriendLink c = all.get(i);
            if (c.getSortOrder() == null || c.getSortOrder() != target) {
                c.setSortOrder(target);
                friendLinkMapper.updateById(c);
            }
        }
    }

    public FriendLink update(FriendLinkDTO dto) {
        if (dto.getId() == null) throw new BizException("id 必填");
        FriendLink f = new FriendLink();
        BeanUtils.copyProperties(dto, f);
        // 位次变化 → 下推冲突
        if (dto.getSortOrder() != null) {
            FriendLink old = friendLinkMapper.selectById(dto.getId());
            if (old != null && !dto.getSortOrder().equals(old.getSortOrder())) {
                pushDownSortOrder(dto.getId(), dto.getSortOrder());
                renumberSortOrder();
            }
        }
        friendLinkMapper.updateById(f);
        return byId(dto.getId());
    }

    public void delete(Long id) { friendLinkMapper.deleteById(id); }

    public void batchDelete(List<Long> ids) { ids.forEach(friendLinkMapper::deleteById); }

    /** 设置/取消推荐（行内快捷操作） */
    public void setRecommended(Long id, boolean recommended) {
        FriendLink f = new FriendLink();
        f.setId(id);
        f.setRecommended(recommended ? 1 : 0);
        friendLinkMapper.updateById(f);
    }

    /** 审核：0 待审核 / 1 已通过 / 2 已拒绝 */
    public void updateStatus(Long id, Integer status) {
        FriendLink f = new FriendLink();
        f.setId(id);
        f.setStatus(status);
        friendLinkMapper.updateById(f);
    }
}
