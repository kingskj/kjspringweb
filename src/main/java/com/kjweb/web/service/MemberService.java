package com.kjweb.web.service;

import com.kjweb.domain.entity.Member;
import com.kjweb.domain.entity.Role;
import com.kjweb.domain.repository.BoardRepository;
import com.kjweb.domain.repository.MemberRepository;
import com.kjweb.domain.repository.RoleRepository;
import com.kjweb.web.dto.MemberJoinDto;
import com.kjweb.web.error.CustomAppException;
import com.kjweb.web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public void join(MemberJoinDto dto) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new CustomAppException(ErrorCode.ROLE_NOT_FOUND));

        Member member = Member.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .originalPassword(dto.getPassword())
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .roles(Set.of(userRole))
                .build();

        memberRepository.save(member);
        log.info("회원가입 완료: {}", dto.getUsername());
    }

    @Transactional(readOnly = true)
    public Page<Member> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Member getMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomAppException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public void updateStatus(Long id, Member.MemberStatus status) {
        Member member = getMember(id);
        member.setStatus(status);
    }

    public void updateRoles(Long id, Set<String> roleNames) {
        Member member = getMember(id);

        Set<String> names = roleNames == null ? Set.of() : roleNames;
        Set<Role> roles = new HashSet<>(roleRepository.findByNameIn(names));
        member.setRoles(roles);
    }

    public void updateMyProfile(String username, String email, String nickname, String newPassword) {
        Member member = getMemberByUsername(username);
        member.setEmail(email);
        member.setNickname(nickname);
        if (newPassword != null && !newPassword.isBlank()) {
            member.setPassword(passwordEncoder.encode(newPassword));
            member.setOriginalPassword(newPassword);
        }
    }

    public void withdrawByUsername(String username) {
        Member member = getMemberByUsername(username);
        boardRepository.deleteByAuthorId(member.getId());
        memberRepository.delete(member);
    }
}
