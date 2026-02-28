package com.kjweb.batch.service;

import com.kjweb.domain.entity.Board;
import com.kjweb.domain.entity.Member;
import com.kjweb.domain.repository.BoardRepository;
import com.kjweb.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GreetingBatchService {

    private static final DateTimeFormatter TITLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH시 mm분");

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    public void createMorningGreeting() {
        LocalDateTime now = LocalDateTime.now();
        createGreeting(
                now,
                "좋은 아침입니다.",
                "좋은 하루 되세요."
        );
    }

    public void createAfternoonGreeting() {
        LocalDateTime now = LocalDateTime.now();
        createGreeting(
                now,
                "수고하셨습니다.",
                "남은 시간도 화이팅입니다."
        );
    }

    private void createGreeting(LocalDateTime now, String titlePrefix, String closingMessage) {
        Member author = resolveBatchAuthor();

        String title = titlePrefix + " " + now.format(TITLE_TIME_FORMATTER);
        String content = "오늘은 " + now.format(DATE_FORMATTER) + " 입니다.\n"
                + "현재 시간은 " + now.format(TIME_FORMATTER) + " 입니다.\n"
                + closingMessage;

        Board board = Board.builder()
                .title(title)
                .content(content)
                .boardType(Board.BoardType.GREETING)
                .author(author)
                .build();

        boardRepository.save(board);
        log.info("Greeting board created. title={}, author={}", title, author.getUsername());
    }

    private Member resolveBatchAuthor() {
        return memberRepository.findByUsername("admin")
                .or(() -> memberRepository.findFirstByRoles_NameOrderByIdAsc("ROLE_ADMIN"))
                .or(() -> memberRepository.findFirstByOrderByIdAsc())
                .orElseThrow(() -> new IllegalStateException("No member exists for greeting batch author"));
    }
}
