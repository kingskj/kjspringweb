package com.kjweb.domain.repository;

import com.kjweb.domain.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    @EntityGraph(attributePaths = "author")
    Page<Board> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Board> findByIsDeletedFalseAndTitleContainingOrderByCreatedAtDesc(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Board> findByIdAndIsDeletedFalse(Long id);

    Page<Board> findByBoardTypeOrderByIdAsc(Board.BoardType boardType, Pageable pageable);

    long deleteByBoardType(Board.BoardType boardType);

    long deleteByAuthorId(Long authorId);
}
