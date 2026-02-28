package com.kjweb.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuBatchRequestDto {
    private List<MenuBatchItemDto> menus;
}
