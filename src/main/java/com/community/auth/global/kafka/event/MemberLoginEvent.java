package com.community.auth.global.kafka.event;

import java.time.Instant;

/**
 * Auth Service → Member Service
 * 토픽: auth.member.login
 *
 * 로그인 성공 시 발행 → Member Service가 lastLoginAt / lastLoginIp 갱신
 */
public record MemberLoginEvent(
        Long memberId,
        String loginIp,
        Instant occurredAt
) {
    public static MemberLoginEvent of(Long memberId, String loginIp) {
        return new MemberLoginEvent(memberId, loginIp, Instant.now());
    }
}
