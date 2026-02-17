package com.kjweb.web.controller;

import com.kjweb.web.dto.BoardWriteDto;
import com.kjweb.web.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        model.addAttribute("boards", boardService.getList(keyword, pageable));
        model.addAttribute("keyword", keyword);
        return "board/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("board", boardService.getDetail(id));
        return "board/detail";
    }

    @GetMapping("/write")
    public String writePage(Model model) {
        model.addAttribute("boardDto", new BoardWriteDto());
        return "board/write";
    }

    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("boardDto") BoardWriteDto dto,
                        BindingResult result,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "board/write";
        }
        var board = boardService.write(dto, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMsg", "게시글이 등록되었습니다");
        return "redirect:/board/" + board.getId();
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        var board = boardService.getDetail(id);
        var dto = new BoardWriteDto();
        dto.setTitle(board.getTitle());
        dto.setContent(board.getContent());
        model.addAttribute("board", board);
        model.addAttribute("boardDto", dto);
        return "board/write";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("boardDto") BoardWriteDto dto,
                       BindingResult result,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "board/write";
        }
        boardService.update(id, dto, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMsg", "게시글이 수정되었습니다");
        return "redirect:/board/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        boardService.delete(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMsg", "게시글이 삭제되었습니다");
        return "redirect:/board";
    }
}
