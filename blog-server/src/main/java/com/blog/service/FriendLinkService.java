package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.FriendLinkApplyDTO;
import com.blog.dto.FriendLinkDTO;
import com.blog.dto.FriendLinkQuery;
import com.blog.entity.FriendLink;
import com.blog.mapper.FriendLinkMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class FriendLinkService {
    private final FriendLinkMapper friendLinkMapper;

    public FriendLinkService(FriendLinkMapper friendLinkMapper) {
        this.friendLinkMapper = friendLinkMapper;
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
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getStatus, 1)
                .orderByAsc(FriendLink::getSortOrder).orderByDesc(FriendLink::getId));
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
        if (f.getSortOrder() == null || f.getSortOrder() == 0) f.setSortOrder(nextSortOrder());
        friendLinkMapper.insert(f);
        return byId(f.getId());
    }

    /** 前台用户提交友链申请，默认待审核 */
    public FriendLink apply(FriendLinkApplyDTO dto) {
        FriendLink f = new FriendLink();
        BeanUtils.copyProperties(dto, f);
        f.setStatus(0);
        f.setSortOrder(nextSortOrder());
        f.setLinkGroup("网友");
        friendLinkMapper.insert(f);
        return byId(f.getId());
    }

    private int nextSortOrder() {
        Integer max = friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                        .select(FriendLink::getSortOrder))
                .stream().map(FriendLink::getSortOrder).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(-1);
        return max + 1;
    }

    public FriendLink update(FriendLinkDTO dto) {
        if (dto.getId() == null) throw new BizException("id 必填");
        FriendLink f = new FriendLink();
        BeanUtils.copyProperties(dto, f);
        friendLinkMapper.updateById(f);
        return byId(dto.getId());
    }

    public void delete(Long id) { friendLinkMapper.deleteById(id); }

    public void batchDelete(List<Long> ids) { ids.forEach(friendLinkMapper::deleteById); }

    /** 审核：0 待审核 / 1 已通过 / 2 已拒绝 */
    public void updateStatus(Long id, Integer status) {
        FriendLink f = new FriendLink();
        f.setId(id);
        f.setStatus(status);
        friendLinkMapper.updateById(f);
    }
}
