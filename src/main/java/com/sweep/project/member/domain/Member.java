package com.sweep.project.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Member {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    private MemberType memberType;
    private Boolean deleted=false;

    @Builder
    public Member(String email,MemberType memberType) {
        this.email = email;
        this.memberType=memberType;
    }
    public void updateDeleted(){
        this.deleted=!this.deleted;
    }
}
