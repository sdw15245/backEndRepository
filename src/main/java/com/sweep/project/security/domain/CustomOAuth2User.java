package com.sweep.project.security.domain;

import com.sweep.project.member.domain.Member;
import com.sweep.project.member.domain.MemberType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
@Getter

public class CustomOAuth2User implements OAuth2User {
    private Long id;
    private MemberType memberType;
    private String email;
    private Member member;

    @Builder
    public CustomOAuth2User(Long id, MemberType memberType, String email, Member member) {
        this.id = id;
        this.memberType = memberType;
        this.email = email;
        this.member=member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    //이거 안쓰긴하는대 뭔가값이 들어있긴 해야되나보다.
    @Override
    public String getName() {
        return "dont use";
    }
}
