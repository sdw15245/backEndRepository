package com.sweep.project.member.service;

import com.sweep.project.member.domain.Member;
import com.sweep.project.security.domain.CustomUserDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityMemberReadService {
    public Member securityMemberRead(){
        CustomUserDetail customUserDetail=
                (CustomUserDetail) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal();
        return customUserDetail.getMember();
    }
}
