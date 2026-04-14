package com.sweep.project.member.repository;

import com.sweep.project.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
@RequiredArgsConstructor
public class MemberRepositoryAdvance {
    private final MemberRepository memberRepository;

    public Member saveMember(Member m){
        return memberRepository.save(m);
    }

    public Boolean checkExist(String email){
        if(memberRepository.existsByEmail(email)){
            throw new RuntimeException("이미 존재하는 회원");
        }
        return true;
    }
    public Optional<Member> findById(Long id){
        return memberRepository.findById(id);
    }

    public Optional<Member> findByEmail(String email){
        return memberRepository.findByEmail(email);
    }
}
