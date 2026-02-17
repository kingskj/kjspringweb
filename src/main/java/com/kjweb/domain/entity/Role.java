package com.kjweb.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String name; // ROLE_ADMIN, ROLE_USER

    @Column(length = 100)
    private String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<Member> members = new HashSet<>();

    // 메뉴 접근 권한 연결
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_menus",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    @Builder.Default
    private Set<Menu> menus = new HashSet<>();
}
