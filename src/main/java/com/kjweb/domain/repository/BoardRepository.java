package com.kjweb.domain.repository;

import com.kjweb.domain.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    Page<Board> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
    Page<Board> findByIsDeletedFalseAndTitleContainingOrderByCreatedAtDesc(String keyword, Pageable pageable);
}
