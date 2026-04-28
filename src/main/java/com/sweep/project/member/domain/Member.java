package com.sweep.project.member.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    @Enumerated(EnumType.STRING)
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
