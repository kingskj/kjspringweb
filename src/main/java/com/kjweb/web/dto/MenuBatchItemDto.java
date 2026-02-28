package com.kjweb.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuBatchItemDto {
    private Long id;
    private String name;
    private String url;
    private Integer sortOrder;
    private Boolean isActive;
    private Long parentId;
    private Boolean deleted;
}
