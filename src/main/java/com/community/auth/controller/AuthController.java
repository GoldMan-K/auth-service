package com.community.auth.controller;

import com.community.auth.dto.ChangePasswordRequest;
import com.community.auth.dto.LoginRequest;
import com.community.auth.dto.TokenResponse;
import com.community.auth.exception.AuthException;
import com.community.auth.global.config.JwtProvider;
import com.community.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API - 로그인, 토큰 관리, 비밀번호 변경")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthController(AuthService authService, JwtProvider jwtProvider) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호로 인증 후 Access Token과 Refresh Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치 / 계정 잠금",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String loginIp = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, loginIp, userAgent));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다. (Token Rotation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token",
                    content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Parameter(description = "Bearer {refreshToken}", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        return ResponseEntity.ok(authService.refresh(extractToken(bearerToken)));
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 폐기합니다. Access Token은 만료 시까지 유효합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰", content = @Content)
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Bearer {refreshToken}", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        authService.logout(extractToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다. 변경 후 모든 Refresh Token이 폐기됩니다.\n\n" +
                    "> Gateway가 JWT 검증 후 `X-Member-Id` 헤더를 주입합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치", content = @Content),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content)
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Gateway가 주입하는 회원 ID", required = true)
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(memberId, request);
        return ResponseEntity.noContent().build();
    }

    // ── private ───────────────────────────────────────────────────────────────

    private String extractToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw AuthException.invalidToken();
        }
        return bearerToken.substring(7);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
