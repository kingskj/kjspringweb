package com.kjweb.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberProfileUpdateDto {
    private String email;
    private String nickname;
    private String newPassword;
}
