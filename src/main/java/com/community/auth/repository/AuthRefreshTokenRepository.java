package com.community.auth.repository;

import com.community.auth.domain.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    /** 회원의 모든 유효한 Refresh Token 폐기 (정지·탈퇴 시) */
    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.revokedAt = :now WHERE t.memberId = :memberId AND t.revokedAt IS NULL")
    void revokeAllByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /** 만료·폐기된 토큰 배치 삭제 */
    @Modifying
    @Query("DELETE FROM AuthRefreshToken t WHERE t.expiresAt < :threshold OR t.revokedAt IS NOT NULL")
    void deleteExpiredAndRevoked(@Param("threshold") LocalDateTime threshold);
}
