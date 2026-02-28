package com.kjweb.web.controller;

import com.kjweb.domain.entity.Member;
import com.kjweb.web.dto.MenuBatchRequestDto;
import com.kjweb.web.service.MemberService;
import com.kjweb.web.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final MemberService memberService;
    private final MenuService menuService;

    @PatchMapping("/members/{id}/status")
    public Map<String, Object> updateMemberStatus(@PathVariable Long id,
                                                  @RequestBody MemberStatusRequest request) {
        memberService.updateStatus(id, request.status());
        return Map.of("success", true, "message", "회원 상태가 변경되었습니다");
    }

    @PatchMapping("/members/{id}/roles")
    public Map<String, Object> updateMemberRoles(@PathVariable Long id,
                                                 @RequestBody MemberRolesRequest request) {
        List<String> roles = request.roles();
        Set<String> roleNames = roles == null ? Set.of() : new HashSet<>(roles);
        memberService.updateRoles(id, roleNames);
        return Map.of("success", true, "message", "회원 권한이 변경되었습니다");
    }

    @PatchMapping("/menus/{id}/active")
    public Map<String, Object> updateMenuActive(@PathVariable Long id,
                                                @RequestBody MenuActiveRequest request) {
        menuService.setActive(id, request.isActive());
        return Map.of("success", true, "message", "메뉴 상태가 변경되었습니다");
    }

    @PutMapping("/menus/batch")
    public Map<String, Object> saveMenusBatch(@RequestBody MenuBatchRequestDto request) {
        menuService.saveMenusBatch(request.getMenus());
        return Map.of("success", true, "message", "메뉴 일괄 저장이 완료되었습니다");
    }

    public record MemberStatusRequest(Member.MemberStatus status) { }

    public record MemberRolesRequest(List<String> roles) { }

    public record MenuActiveRequest(Boolean isActive) { }
}
