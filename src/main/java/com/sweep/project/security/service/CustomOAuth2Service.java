package com.sweep.project.security.service;

import com.sweep.project.member.domain.Member;
import com.sweep.project.member.repository.MemberRepositoryAdvance;
import com.sweep.project.member.domain.MemberType;
import com.sweep.project.security.domain.CustomOAuth2User;
import com.sweep.project.security.domain.GoogleResponse;
import com.sweep.project.security.domain.KakaoResponse;
import com.sweep.project.security.domain.OAuth2Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
public class CustomOAuth2Service extends DefaultOAuth2UserService {
    @Value("${spring.user.imgurl}")
    private String basicImgUrl;
    private MemberRepositoryAdvance memberRepository;

    public CustomOAuth2Service(MemberRepositoryAdvance memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User userData=super.loadUser(userRequest);

        String registrationId=userRequest.getClientRegistration().getRegistrationId();

        OAuth2Response response=createOAuth2Response(registrationId,userData);

        if(response==null){
            throw new RuntimeException();
        }
        else{
            log.info("{}-유저가 로그인 시도",registrationId);
            Optional<Member> member=memberRepository.findByEmail(response.getEmail());
            if(member.isPresent()){
                return whenExistMember(member.get(),response.getProvider());
            }
            log.info("기존에 없는 {}-{}가 로그인 시도",response.getProvider(),response.getEmail());
            return whenNotExistMember(response);
        }

    }
    private OAuth2User whenExistMember(Member member, MemberType provider){
        log.info("기존에 존재하는 {}-{} 가 로그인 시도",provider,member.getEmail());
        return CustomOAuth2User
                .builder()
                .id(member.getId())
                .member(member)
                .email(member.getEmail())
                .memberType(member.getMemberType())
                .build();
    }

    @Transactional
    private OAuth2User whenNotExistMember(OAuth2Response response){
        Member newMember=Member.builder()
                .email(response.getEmail())
                .memberType(response.getProvider())
                .build();

        newMember=memberRepository.saveMember(newMember);
        return CustomOAuth2User
                .builder()
                .id(newMember.getId())
                .member(newMember)
                .email(newMember.getEmail())
                .memberType(newMember.getMemberType())
                .build();
    }

    private OAuth2Response createOAuth2Response(String registrationId,OAuth2User userData){
        switch (registrationId){
            case "google"->{
                return new GoogleResponse(userData.getAttributes());
            }
            case "kakao"->{
                return new KakaoResponse(userData.getAttributes());
            }
            default ->{
                return null;
            }
        }
    }
}
