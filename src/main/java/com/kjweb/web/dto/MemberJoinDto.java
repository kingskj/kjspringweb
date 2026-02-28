package com.kjweb.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberJoinDto {

    private String username;

    private String password;

    private String passwordConfirm;

    private String email;

    private String nickname;
}
