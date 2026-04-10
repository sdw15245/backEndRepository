package com.sweep.project.member.service;

import com.sweep.project.member.domain.Member;
import com.sweep.project.member.repository.MemberRepositoryAdvance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final SecurityMemberReadService securityMemberReadService;
    private final MemberRepositoryAdvance memberRepositoryAdvance;


    public void deleteMember(){
        Member member=securityMemberReadService.securityMemberRead();
        Optional<Member> member1=memberRepositoryAdvance.findById(member.getId());
        if(member1.isEmpty()){
            throw new RuntimeException("존재하지 않는 회원");
        }
        member1.get().updateDeleted();
    }
}
