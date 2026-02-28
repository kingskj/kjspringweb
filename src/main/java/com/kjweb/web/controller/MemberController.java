package com.kjweb.web.controller;

import com.kjweb.domain.entity.Member;
import com.kjweb.web.dto.MemberProfileUpdateDto;
import com.kjweb.web.error.CustomAppException;
import com.kjweb.web.error.ErrorCode;
import com.kjweb.web.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        String username = currentUsername(authentication);
        Member member = memberService.getMemberByUsername(username);

        MemberProfileUpdateDto profileDto = new MemberProfileUpdateDto();
        profileDto.setEmail(member.getEmail());
        profileDto.setNickname(member.getNickname());

        model.addAttribute("member", member);
        model.addAttribute("profileDto", profileDto);
        return "member/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @ModelAttribute("profileDto") MemberProfileUpdateDto profileDto,
                                RedirectAttributes redirectAttributes) {
        String username = currentUsername(authentication);
        memberService.updateMyProfile(username, profileDto.getEmail(), profileDto.getNickname(), profileDto.getNewPassword());
        redirectAttributes.addFlashAttribute("successMsg", "회원정보가 수정되었습니다");
        return "redirect:/member/profile";
    }

    @PostMapping("/withdraw")
    public String withdraw(Authentication authentication,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        String username = currentUsername(authentication);
        memberService.withdrawByUsername(username);

        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, authentication);
        return "redirect:/";
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new CustomAppException(ErrorCode.FORBIDDEN);
        }
        return authentication.getName();
    }
}
