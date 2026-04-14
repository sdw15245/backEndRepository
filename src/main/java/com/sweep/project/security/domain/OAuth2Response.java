package com.sweep.project.security.domain;

import com.sweep.project.member.domain.MemberType;

public interface OAuth2Response {
    MemberType getProvider();
    String getEmail();
}
