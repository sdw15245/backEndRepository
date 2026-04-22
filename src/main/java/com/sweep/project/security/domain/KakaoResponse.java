package com.sweep.project.security.domain;

import com.sweep.project.member.domain.MemberType;

import java.util.Map;

public class KakaoResponse implements OAuth2Response{

    private final Map<String, Object> kakaoAccount;

    public KakaoResponse(Map<String, Object> attributes) {
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    }
    @Override
    public MemberType getProvider() {
        return MemberType.KAKAO;
    }
    @Override
    public String getEmail() {
        return (String) kakaoAccount.get("email");
    }

}
