package com.kjweb.domain.repository;

import com.kjweb.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    Optional<Member> findByEmail(String email);
    Optional<Member> findFirstByRoles_NameOrderByIdAsc(String roleName);
    Optional<Member> findFirstByOrderByIdAsc();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
