package com.community.auth.global.kafka.event;

import java.time.Instant;

/**
 * Member Service → Auth Service
 * 토픽: member.created
 *
 * 회원 가입 완료 시 수신 → auth_credential 레코드 생성 트리거
 * (실제 password_hash는 회원가입 API에서 직접 생성하므로 여기선 memberId·username만 사용)
 */
public record MemberCreatedEvent(
        Long memberId,
        String username,
        Instant occurredAt
) {}
