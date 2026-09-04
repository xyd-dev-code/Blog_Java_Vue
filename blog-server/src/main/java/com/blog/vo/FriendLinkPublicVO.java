package com.blog.vo;

import com.blog.entity.FriendLink;
import java.util.List;

/** Public friend-link fields. Applicant email and moderation metadata stay private. */
public record FriendLinkPublicVO(
        Long id,
        String name,
        String url,
        String avatar,
        String description,
        String linkGroup,
        Integer sortOrder,
        Integer recommended) {

    public static FriendLinkPublicVO from(FriendLink link) {
        if (link == null) return null;
        return new FriendLinkPublicVO(
                link.getId(), link.getName(), link.getUrl(), link.getAvatar(),
                link.getDescription(), link.getLinkGroup(), link.getSortOrder(), link.getRecommended());
    }

    public static List<FriendLinkPublicVO> list(List<FriendLink> links) {
        return links == null ? List.of() : links.stream().map(FriendLinkPublicVO::from).toList();
    }
}
