package com.kjweb.web.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST("E400", "잘못된 요청입니다", HttpStatus.BAD_REQUEST),
    FORBIDDEN("E403", "요청 권한이 없습니다", HttpStatus.FORBIDDEN),
    MEMBER_NOT_FOUND("E404_MEMBER", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    BOARD_NOT_FOUND("E404_BOARD", "게시글을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    MENU_NOT_FOUND("E404_MENU", "메뉴를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND("E404_ROLE", "권한 정보를 찾을 수 없습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND("E404_USER", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
