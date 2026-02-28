package com.kjweb.web.error;

import com.kjweb.web.controller.AdminController;
import com.kjweb.web.controller.AuthController;
import com.kjweb.web.controller.BoardController;
import com.kjweb.web.controller.HomeController;
import com.kjweb.web.controller.MemberController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

@ControllerAdvice(assignableTypes = {
        AdminController.class,
        AuthController.class,
        BoardController.class,
        HomeController.class,
        MemberController.class
})
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomAppException.class)
    public String handleCustomException(CustomAppException ex,
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        RedirectAttributes redirectAttributes) {
        ErrorCode errorCode = ex.getErrorCode();
        response.setStatus(errorCode.getStatus().value());

        log.error("[{}] {} - path: {}", errorCode.getCode(), ex.getDetailMessage(), request.getRequestURI(), ex);
        redirectAttributes.addFlashAttribute("errorMsg", ex.getDetailMessage());
        return "redirect:" + resolveRedirectPath(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  RedirectAttributes redirectAttributes) {
        response.setStatus(500);

        log.error("[500] {} - path: {}", ex.getMessage(), request.getRequestURI(), ex);
        String message = ex.getMessage() == null ? "서버 오류가 발생했습니다" : ex.getMessage();
        redirectAttributes.addFlashAttribute("errorMsg", message);
        return "redirect:" + resolveRedirectPath(request);
    }

    private String resolveRedirectPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            String query = uri.getQuery();
            if (path == null || path.isBlank()) {
                return "/";
            }
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (Exception e) {
            return "/";
        }
    }
}
