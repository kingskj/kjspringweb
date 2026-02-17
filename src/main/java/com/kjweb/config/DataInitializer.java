package com.kjweb.config;

import com.kjweb.domain.entity.Member;
import com.kjweb.domain.entity.Menu;
import com.kjweb.domain.entity.Role;
import com.kjweb.domain.repository.MemberRepository;
import com.kjweb.domain.repository.MenuRepository;
import com.kjweb.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DataInitializer 시작 ===");

        // 역할 생성
        Role adminRole = createRoleIfNotExists("ROLE_ADMIN", "관리자");
        Role userRole = createRoleIfNotExists("ROLE_USER", "일반사용자");

        // 기본 메뉴 생성
        createMenuIfNotExists("홈", "/", 1, null);
        createMenuIfNotExists("게시판", "/board", 2, null);
        Menu adminMenu = createMenuIfNotExists("관리자", "/admin", 3, null);
        createMenuIfNotExists("회원관리", "/admin/members", 1, adminMenu);
        createMenuIfNotExists("메뉴관리", "/admin/menus", 2, adminMenu);
        createMenuIfNotExists("권한관리", "/admin/roles", 3, adminMenu);

        // 관리자 계정
        if (!memberRepository.existsByUsername("admin")) {
            Member admin = Member.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .email("admin@kjweb.com")
                    .nickname("관리자")
                    .roles(Set.of(adminRole, userRole))
                    .build();
            memberRepository.save(admin);
            log.info("관리자 계정 생성: admin / admin1234");
        }

        // 테스트 계정
        if (!memberRepository.existsByUsername("user1")) {
            Member user = Member.builder()
                    .username("user1")
                    .password(passwordEncoder.encode("user1234"))
                    .email("user1@kjweb.com")
                    .nickname("테스트유저")
                    .roles(Set.of(userRole))
                    .build();
            memberRepository.save(user);
            log.info("테스트 계정 생성: user1 / user1234");
        }

        log.info("=== DataInitializer 완료 ===");
    }

    private Role createRoleIfNotExists(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder().name(name).description(description).build();
            return roleRepository.save(role);
        });
    }

    private Menu createMenuIfNotExists(String name, String url, int sortOrder, Menu parent) {
        Menu menu = Menu.builder()
                .name(name)
                .url(url)
                .sortOrder(sortOrder)
                .parent(parent)
                .isActive(true)
                .build();
        return menuRepository.save(menu);
    }
}
