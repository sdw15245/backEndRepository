package com.sweep.project.security.domain;

import com.sweep.project.member.domain.MemberType;

import java.util.Map;

public class GoogleResponse implements OAuth2Response {

    private Map<String,Object> attrs;

    public GoogleResponse(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    @Override
    public MemberType getProvider() {
        return MemberType.GOOGLE;
    }

    @Override
    public String getEmail() {
        return (String) attrs.get("email");
    }
}


