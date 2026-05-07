package com.community.auth.service;

import com.community.auth.domain.AuthCredential;
import com.community.auth.domain.AuthLoginHistory;
import com.community.auth.domain.AuthRefreshToken;
import com.community.auth.dto.ChangePasswordRequest;
import com.community.auth.dto.LoginRequest;
import com.community.auth.dto.TokenResponse;
import com.community.auth.exception.AuthException;
import com.community.auth.global.config.JwtProvider;
import com.community.auth.global.kafka.producer.AuthEventPublisher;
import com.community.auth.repository.AuthCredentialRepository;
import com.community.auth.repository.AuthLoginHistoryRepository;
import com.community.auth.repository.AuthRefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final int MAX_FAILED_COUNT = 5;
    private static final int LOCK_MINUTES = 30;

    private final AuthCredentialRepository credentialRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventPublisher eventPublisher;

    public AuthService(AuthCredentialRepository credentialRepository,
                       AuthRefreshTokenRepository refreshTokenRepository,
                       AuthLoginHistoryRepository loginHistoryRepository,
                       JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder,
                       AuthEventPublisher eventPublisher) {
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    /**
     * POST /api/auth/login — 로그인 → Access Token + Refresh Token 발급
     */
    @Transactional
    public TokenResponse login(LoginRequest request, String loginIp, String userAgent) {
        AuthCredential credential = credentialRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    saveLoginHistory(null, request.username(), loginIp, userAgent, false, "CREDENTIAL_NOT_FOUND");
                    return AuthException.invalidCredentials();
                });

        // 계정 잠금 확인
        if (credential.isLocked()) {
            saveLoginHistory(credential.getMemberId(), request.username(), loginIp, userAgent, false, "ACCOUNT_LOCKED");
            throw AuthException.accountLocked();
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            credential.incrementFailedLogin();
            if (credential.getFailedLoginCount() >= MAX_FAILED_COUNT) {
                credential.lockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            saveLoginHistory(credential.getMemberId(), request.username(), loginIp, userAgent, false, "INVALID_PASSWORD");
            throw AuthException.invalidCredentials();
        }

        // 로그인 성공
        credential.resetFailedLogin();
        saveLoginHistory(credential.getMemberId(), request.username(), loginIp, userAgent, true, null);

        // 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(credential.getMemberId(), credential.getUsername(), "USER");
        String refreshToken = jwtProvider.generateRefreshToken(credential.getMemberId());

        // Refresh Token 저장 (해시)
        String tokenHash = hashToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpiry());
        refreshTokenRepository.save(new AuthRefreshToken(credential.getMemberId(), tokenHash, expiresAt));

        // Member Service에 로그인 이벤트 발행
        eventPublisher.publishMemberLogin(credential.getMemberId(), loginIp);

        return TokenResponse.of(accessToken, refreshToken, jwtProvider.getRefreshTokenExpiry());
    }

    /**
     * POST /api/auth/refresh — Refresh Token으로 Access Token 재발급
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw AuthException.invalidToken();
        }

        String tokenHash = hashToken(refreshToken);
        AuthRefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(AuthException::invalidToken);

        if (!storedToken.isValid()) {
            throw AuthException.invalidToken();
        }

        Long memberId = storedToken.getMemberId();
        AuthCredential credential = credentialRepository.findById(memberId)
                .orElseThrow(() -> AuthException.credentialNotFound(memberId));

        // 기존 Refresh Token 폐기 후 새로 발급 (Token Rotation)
        storedToken.revoke();

        String newAccessToken = jwtProvider.generateAccessToken(memberId, credential.getUsername(), "USER");
        String newRefreshToken = jwtProvider.generateRefreshToken(memberId);

        String newTokenHash = hashToken(newRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpiry());
        refreshTokenRepository.save(new AuthRefreshToken(memberId, newTokenHash, expiresAt));

        return TokenResponse.of(newAccessToken, newRefreshToken, jwtProvider.getRefreshTokenExpiry());
    }

    /**
     * POST /api/auth/logout — Refresh Token 폐기
     */
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(AuthRefreshToken::revoke);
    }

    /**
     * POST /api/auth/password/change — 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        AuthCredential credential = credentialRepository.findById(memberId)
                .orElseThrow(() -> AuthException.credentialNotFound(memberId));

        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }

        credential.changePassword(passwordEncoder.encode(request.newPassword()));

        // 비밀번호 변경 시 모든 Refresh Token 폐기 (보안 강화)
        refreshTokenRepository.revokeAllByMemberId(memberId, LocalDateTime.now());
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void saveLoginHistory(Long memberId, String username, String loginIp,
                                  String userAgent, boolean success, String failureReason) {
        AuthLoginHistory history = success
                ? AuthLoginHistory.success(memberId, username, loginIp, userAgent)
                : AuthLoginHistory.failure(username, loginIp, userAgent, failureReason);
        loginHistoryRepository.save(history);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
