package com.kjweb.web.dto;

import com.kjweb.domain.entity.Board;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BoardWriteDto {

    private String title;

    private String content;

    private Board.BoardType boardType;
}
