package com.kjweb.web.controller;

import com.kjweb.domain.entity.Member;
import com.kjweb.web.service.MemberService;
import com.kjweb.web.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;
    private final MenuService menuService;

    @GetMapping
    public String dashboard() {
        return "admin/dashboard";
    }

    // 회원 관리
    @GetMapping("/members")
    public String members(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("members", memberService.getMembers(PageRequest.of(page, 20)));
        return "admin/members";
    }

    @PostMapping("/members/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Member.MemberStatus status,
                               RedirectAttributes redirectAttributes) {
        memberService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMsg", "회원 상태가 변경되었습니다");
        return "redirect:/admin/members";
    }

    // 메뉴 관리
    @GetMapping("/menus")
    public String menus(Model model) {
        model.addAttribute("menus", menuService.getAllMenus());
        return "admin/menus";
    }

    @PostMapping("/menus/{id}/toggle")
    public String toggleMenu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        menuService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMsg", "메뉴 상태가 변경되었습니다");
        return "redirect:/admin/menus";
    }
}
