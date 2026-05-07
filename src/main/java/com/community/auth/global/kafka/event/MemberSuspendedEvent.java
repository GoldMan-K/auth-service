package com.community.auth.global.kafka.event;

import java.time.Instant;

/**
 * Member Service → Auth Service
 * 토픽: member.suspended
 *
 * 회원 정지 시 수신 → 해당 회원의 모든 Refresh Token 강제 폐기
 */
public record MemberSuspendedEvent(
        Long memberId,
        Instant occurredAt
) {}
