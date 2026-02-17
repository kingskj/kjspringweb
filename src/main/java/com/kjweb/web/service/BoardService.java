package com.kjweb.web.service;

import com.kjweb.domain.entity.Board;
import com.kjweb.domain.entity.Member;
import com.kjweb.domain.repository.BoardRepository;
import com.kjweb.domain.repository.MemberRepository;
import com.kjweb.web.dto.BoardWriteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    public Board write(BoardWriteDto dto, String username) {
        Member author = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

        Board board = Board.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .author(author)
                .build();

        return boardRepository.save(board);
    }

    @Transactional(readOnly = true)
    public Page<Board> getList(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return boardRepository.findByIsDeletedFalseAndTitleContainingOrderByCreatedAtDesc(keyword, pageable);
        }
        return boardRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Board getDetail(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));
        board.increaseViewCount();
        return board;
    }

    public Board update(Long id, BoardWriteDto dto, String username) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        if (!board.getAuthor().getUsername().equals(username)) {
            throw new SecurityException("수정 권한이 없습니다");
        }

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        return board;
    }

    public void delete(Long id, String username) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        boolean isAdmin = memberRepository.findByUsername(username)
                .map(m -> m.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .orElse(false);

        if (!board.getAuthor().getUsername().equals(username) && !isAdmin) {
            throw new SecurityException("삭제 권한이 없습니다");
        }

        board.setIsDeleted(true);
    }
}
