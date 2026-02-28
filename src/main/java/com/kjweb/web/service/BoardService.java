package com.kjweb.web.service;

import com.kjweb.domain.entity.Board;
import com.kjweb.domain.entity.Member;
import com.kjweb.domain.repository.BoardRepository;
import com.kjweb.domain.repository.MemberRepository;
import com.kjweb.web.dto.BoardWriteDto;
import com.kjweb.web.error.CustomAppException;
import com.kjweb.web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                .orElseThrow(() -> new CustomAppException(ErrorCode.USER_NOT_FOUND));

        boolean admin = isAdmin(username);
        Board.BoardType boardType = resolveBoardType(dto.getBoardType(), admin);

        Board board = Board.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(boardType)
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
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.BOARD_NOT_FOUND));
        board.increaseViewCount();
        return board;
    }

    @Transactional(readOnly = true)
    public Board getEditableBoard(Long id, String username) {
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.BOARD_NOT_FOUND));

        if (!canManageBoard(board, username)) {
            throw new CustomAppException(ErrorCode.FORBIDDEN, "No permission to edit this post");
        }

        return board;
    }

    public Board update(Long id, BoardWriteDto dto, String username) {
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.BOARD_NOT_FOUND));

        boolean admin = isAdmin(username);
        boolean owner = board.getAuthor().getUsername().equals(username);
        if (!owner && !admin) {
            throw new CustomAppException(ErrorCode.FORBIDDEN, "No permission to edit this post");
        }

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        if (admin && owner) {
            board.setBoardType(resolveBoardType(dto.getBoardType(), true));
        } else if (!admin) {
            // Normal users are always fixed to GENERAL.
            board.setBoardType(Board.BoardType.GENERAL);
        }
        return board;
    }

    public void delete(Long id, String username) {
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.BOARD_NOT_FOUND));

        if (!canManageBoard(board, username)) {
            throw new CustomAppException(ErrorCode.FORBIDDEN, "No permission to delete this post");
        }

        board.setIsDeleted(true);
    }

    private boolean canManageBoard(Board board, String username) {
        return board.getAuthor().getUsername().equals(username) || isAdmin(username);
    }

    private boolean isAdmin(String username) {
        return memberRepository.findByUsername(username)
                .map(m -> m.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .orElse(false);
    }

    private Board.BoardType resolveBoardType(Board.BoardType boardType, boolean isAdmin) {
        if (!isAdmin) {
            return Board.BoardType.GENERAL;
        }
        return boardType == null ? Board.BoardType.GENERAL : boardType;
    }
}
