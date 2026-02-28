package com.kjweb.config;

import com.kjweb.domain.entity.Member;
import com.kjweb.domain.entity.Menu;
import com.kjweb.domain.entity.Role;
import com.kjweb.domain.repository.BoardRepository;
import com.kjweb.domain.repository.MemberRepository;
import com.kjweb.domain.repository.MenuRepository;
import com.kjweb.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final BoardRepository boardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DataInitializer 시작 ===");
        ensureBatchSchema();
        enforceMenuConstraints();
        enforceBoardConstraints();
        logTableStatus();
        seedRolesIfEmpty();
        seedMenusIfEmpty();
        seedMembersIfEmpty();
        log.info("=== DataInitializer 완료 ===");
    }

    private void ensureBatchSchema() {
        try {
            Integer batchTableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'BATCH_JOB_INSTANCE'",
                    Integer.class
            );
            if (batchTableCount != null && batchTableCount > 0) {
                log.info("spring batch metadata schema already exists");
                return;
            }
            if (jdbcTemplate.getDataSource() == null) {
                log.warn("spring batch metadata schema init skipped: datasource is null");
                return;
            }

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
            DatabasePopulatorUtils.execute(populator, jdbcTemplate.getDataSource());
            log.info("spring batch metadata schema initialized");
        } catch (Exception e) {
            log.warn("spring batch metadata schema init failed: {}", e.getMessage());
        }
    }

    private void enforceMenuConstraints() {
        try {
            jdbcTemplate.update("UPDATE menus SET name = '[NO_NAME]' WHERE name IS NULL OR TRIM(name) = ''");
            jdbcTemplate.update("UPDATE menus SET url = '/menu/' || id WHERE url IS NULL OR TRIM(url) = ''");
            jdbcTemplate.update("UPDATE menus SET sort_order = 0 WHERE sort_order IS NULL");
            jdbcTemplate.update("UPDATE menus SET is_active = TRUE WHERE is_active IS NULL");

            List<String> duplicateUrls = jdbcTemplate.queryForList(
                    "SELECT url FROM menus GROUP BY url HAVING COUNT(*) > 1",
                    String.class
            );
            for (String duplicateUrl : duplicateUrls) {
                List<Long> ids = jdbcTemplate.queryForList(
                        "SELECT id FROM menus WHERE url = ? ORDER BY id",
                        Long.class,
                        duplicateUrl
                );
                for (int i = 1; i < ids.size(); i++) {
                    Long id = ids.get(i);
                    jdbcTemplate.update("UPDATE menus SET url = ? WHERE id = ?", duplicateUrl + "-dup-" + id, id);
                }
            }

            jdbcTemplate.execute("ALTER TABLE menus ALTER COLUMN name VARCHAR(50) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE menus ALTER COLUMN url VARCHAR(200) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE menus ALTER COLUMN sort_order INT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE menus ALTER COLUMN is_active BOOLEAN NOT NULL");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_menus_url ON menus(url)");
            log.info("menus 제약조건 적용 완료 (name/url/sort_order/is_active NOT NULL, url UNIQUE)");
        } catch (Exception e) {
            log.warn("menus 제약조건 적용 실패: {}", e.getMessage());
        }
    }

    private void enforceBoardConstraints() {
        try {
            Integer hasBoardTypeColumn = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BOARDS' AND COLUMN_NAME = 'BOARD_TYPE'",
                    Integer.class
            );
            if (hasBoardTypeColumn == null || hasBoardTypeColumn == 0) {
                jdbcTemplate.execute("ALTER TABLE boards ADD COLUMN board_type VARCHAR(20) DEFAULT 'GENERAL'");
            }
            jdbcTemplate.update("UPDATE boards SET board_type = 'GENERAL' WHERE board_type IS NULL OR TRIM(board_type) = ''");
            jdbcTemplate.execute("ALTER TABLE boards ALTER COLUMN board_type VARCHAR(20) NOT NULL");
            log.info("boards constraints applied (board_type NOT NULL)");
        } catch (Exception e) {
            log.warn("boards constraints apply failed: {}", e.getMessage());
        }
    }

    private void logTableStatus() {
        log.info("테이블 점검: members={}, roles={}, menus={}, boards={}",
                memberRepository.count(),
                roleRepository.count(),
                menuRepository.count(),
                boardRepository.count());
    }

    private void seedRolesIfEmpty() {
        if (roleRepository.count() > 0) {
            log.info("roles 테이블 데이터 존재 -> 기본 role 시드 건너뜀");
            return;
        }

        roleRepository.save(Role.builder().name("ROLE_ADMIN").description("관리자").build());
        roleRepository.save(Role.builder().name("ROLE_USER").description("일반사용자").build());
        log.info("기본 역할 생성 완료");
    }

    private void seedMenusIfEmpty() {
        if (menuRepository.count() > 0) {
            log.info("menus 테이블 데이터 존재 -> 기본 menu 시드 건너뜀");
            return;
        }

        Menu home = createMenu("홈", "/", 1, null);
        createMenu("게시판", "/board", 2, null);
        Menu admin = createMenu("관리자", "/admin", 3, null);
        createMenu("회원관리", "/admin/members", 1, admin);
        createMenu("메뉴관리", "/admin/menus", 2, admin);
        createMenu("권한관리", "/admin/roles", 3, admin);
        if (home != null) {
            log.info("기본 메뉴 생성 완료");
        }
    }

    private void seedMembersIfEmpty() {
        if (memberRepository.count() > 0) {
            log.info("members 테이블 데이터 존재 -> 기본 계정 시드 건너뜀");
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_ADMIN")
                        .description("관리자")
                        .build()));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .description("일반사용자")
                        .build()));

        Member admin = Member.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .originalPassword("admin1234")
                .email("admin@kjweb.com")
                .nickname("관리자")
                .roles(Set.of(adminRole, userRole))
                .build();
        memberRepository.save(admin);

        Member user = Member.builder()
                .username("user1")
                .password(passwordEncoder.encode("user1234"))
                .originalPassword("user1234")
                .email("user1@kjweb.com")
                .nickname("테스트유저")
                .roles(Set.of(userRole))
                .build();
        memberRepository.save(user);

        log.info("기본 계정 생성 완료: admin/user1");
    }

    private Menu createMenu(String name, String url, int sortOrder, Menu parent) {
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
