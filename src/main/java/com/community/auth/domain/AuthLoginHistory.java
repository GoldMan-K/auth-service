package com.community.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "auth_login_history",
    indexes = {
        @Index(name = "idx_login_history_member_id", columnList = "member_id"),
        @Index(name = "idx_login_history_created_at", columnList = "created_at")
    }
)
public class AuthLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(length = 50)
    private String username;

    @Column(name = "login_ip", length = 45)
    private String loginIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "success_yn", nullable = false, length = 1)
    private String successYn;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuthLoginHistory() {}

    private AuthLoginHistory(Long memberId, String username, String loginIp,
                             String userAgent, String successYn, String failureReason) {
        this.memberId = memberId;
        this.username = username;
        this.loginIp = loginIp;
        this.userAgent = userAgent;
        this.successYn = successYn;
        this.failureReason = failureReason;
    }

    public static AuthLoginHistory success(Long memberId, String username, String loginIp, String userAgent) {
        return new AuthLoginHistory(memberId, username, loginIp, userAgent, "Y", null);
    }

    public static AuthLoginHistory failure(String username, String loginIp, String userAgent, String reason) {
        return new AuthLoginHistory(null, username, loginIp, userAgent, "N", reason);
    }

    // Getters
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getUsername() { return username; }
    public String getLoginIp() { return loginIp; }
    public String getUserAgent() { return userAgent; }
    public String getSuccessYn() { return successYn; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
