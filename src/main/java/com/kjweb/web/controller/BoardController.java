package com.kjweb.web.controller;

import com.kjweb.domain.entity.Board;
import com.kjweb.web.dto.BoardWriteDto;
import com.kjweb.web.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String writePage(@AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        BoardWriteDto dto = new BoardWriteDto();
        dto.setBoardType(Board.BoardType.GENERAL);
        model.addAttribute("boardDto", dto);
        model.addAttribute("canEditBoardType", hasAdminRole(userDetails));
        return "board/write";
    }

    @PostMapping("/write")
    public String write(@ModelAttribute("boardDto") BoardWriteDto dto,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        var board = boardService.write(dto, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMsg", "게시글이 등록되었습니다");
        return "redirect:/board/" + board.getId();
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        var board = boardService.getEditableBoard(id, userDetails.getUsername());
        boolean isAdmin = hasAdminRole(userDetails);
        boolean isOwner = board.getAuthor().getUsername().equals(userDetails.getUsername());
        boolean canEditBoardType = isAdmin && isOwner;

        var dto = new BoardWriteDto();
        dto.setTitle(board.getTitle());
        dto.setContent(board.getContent());
        if (!isAdmin) {
            dto.setBoardType(Board.BoardType.GENERAL);
        } else {
            dto.setBoardType(board.getBoardType() == null ? Board.BoardType.GENERAL : board.getBoardType());
        }
        model.addAttribute("board", board);
        model.addAttribute("boardDto", dto);
        model.addAttribute("canEditBoardType", canEditBoardType);
        return "board/write";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @ModelAttribute("boardDto") BoardWriteDto dto,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
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

    private boolean hasAdminRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
