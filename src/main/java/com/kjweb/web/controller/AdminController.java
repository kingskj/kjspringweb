package com.kjweb.web.controller;

import com.kjweb.domain.entity.Member;
import com.kjweb.domain.entity.Menu;
import com.kjweb.web.service.MemberService;
import com.kjweb.web.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;
    private final MenuService menuService;

    @ModelAttribute("adminSideMenus")
    public List<Menu> adminSideMenus() {
        return menuService.getAdminSideMenus();
    }

    @ModelAttribute("adminCurrentPath")
    public String adminCurrentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @GetMapping
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("members", memberService.getMembers(PageRequest.of(page, 20)));
        return "admin/members";
    }

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("members", memberService.getAllMembers());
        model.addAttribute("roles", memberService.getAllRoles());
        return "admin/roles";
    }

    @PostMapping("/members/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Member.MemberStatus status,
                               RedirectAttributes redirectAttributes) {
        memberService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMsg", "회원 상태가 변경되었습니다");
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(name = "roles", required = false) List<String> roles,
                              RedirectAttributes redirectAttributes) {
        Set<String> roleNames = roles == null ? Set.of() : new HashSet<>(roles);
        memberService.updateRoles(id, roleNames);
        redirectAttributes.addFlashAttribute("successMsg", "회원 권한이 변경되었습니다");
        return "redirect:/admin/roles";
    }

    @GetMapping("/menus")
    public String menus(Model model) {
        model.addAttribute("menus", menuService.getAllMenus());
        model.addAttribute("adminMenuId", menuService.getAdminMenuId());
        return "admin/menus";
    }

    @PostMapping("/menus/{id}/toggle")
    public String toggleMenu(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        menuService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMsg", "메뉴 상태가 변경되었습니다");
        return "redirect:/admin/menus";
    }
}
