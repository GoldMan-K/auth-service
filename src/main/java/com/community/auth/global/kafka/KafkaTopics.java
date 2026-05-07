package com.community.auth.global.kafka;

/**
 * Kafka 토픽 상수
 *
 * [Publish]  Auth Service → 외부 서비스
 * [Consume]  외부 서비스  → Auth Service
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // ── Publish (Auth Service 발행) ────────────────────────────────────────────

    /** 로그인 성공 → Member Service (lastLoginAt / lastLoginIp 갱신) */
    public static final String AUTH_MEMBER_LOGIN = "auth.member.login";

    // ── Consume (Auth Service 수신) ────────────────────────────────────────────

    /** Member Service 발행 → 회원 가입 완료 시 credential 생성 */
    public static final String MEMBER_CREATED = "member.created";

    /** Member Service 발행 → 회원 정지 시 Refresh Token 강제 폐기 */
    public static final String MEMBER_SUSPENDED = "member.suspended";
}
