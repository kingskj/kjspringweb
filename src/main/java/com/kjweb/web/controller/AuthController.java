package com.kjweb.web.controller;

import com.kjweb.web.dto.MemberJoinDto;
import com.kjweb.web.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "아이디 또는 비밀번호가 올바르지 않습니다");
        }
        return "auth/login";
    }

    @GetMapping("/join")
    public String joinPage(Model model) {
        model.addAttribute("joinDto", new MemberJoinDto());
        return "auth/join";
    }

    @PostMapping("/join")
    public String join(@Valid @ModelAttribute("joinDto") MemberJoinDto dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (result.hasErrors()) {
            return "auth/join";
        }
        try {
            memberService.join(dto);
            redirectAttributes.addFlashAttribute("successMsg", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/join";
        }
    }
}
