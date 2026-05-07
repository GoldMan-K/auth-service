package com.community.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    public static AuthException accountLocked() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "계정이 잠겨 있습니다. 잠시 후 다시 시도해주세요.");
    }

    public static AuthException invalidToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    public static AuthException credentialNotFound(Long memberId) {
        return new AuthException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다. memberId=" + memberId);
    }
}
