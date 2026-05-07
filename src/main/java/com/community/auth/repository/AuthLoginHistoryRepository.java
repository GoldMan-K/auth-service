package com.community.auth.repository;

import com.community.auth.domain.AuthLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuthLoginHistoryRepository extends JpaRepository<AuthLoginHistory, Long> {

    /** 6개월 이상된 로그인 이력 배치 삭제 */
    @Modifying
    @Query("DELETE FROM AuthLoginHistory h WHERE h.createdAt < :threshold")
    void deleteOlderThan(@Param("threshold") LocalDateTime threshold);
}
