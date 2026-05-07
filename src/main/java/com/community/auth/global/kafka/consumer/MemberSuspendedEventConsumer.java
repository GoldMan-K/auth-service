package com.community.auth.global.kafka.consumer;

import com.community.auth.global.kafka.KafkaTopics;
import com.community.auth.global.kafka.event.MemberSuspendedEvent;
import com.community.auth.repository.AuthRefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Member Service → Auth Service
 * 토픽: member.suspended
 *
 * 회원 정지 이벤트 수신 → 해당 회원의 모든 유효한 Refresh Token을 즉시 폐기.
 * 이후 토큰 재발급 시 정지된 회원임을 Member Service에 확인하거나
 * credential 조회 시 차단 처리를 통해 로그인을 막는다.
 */
@Component
public class MemberSuspendedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MemberSuspendedEventConsumer.class);

    private final AuthRefreshTokenRepository refreshTokenRepository;

    public MemberSuspendedEventConsumer(AuthRefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @KafkaListener(
            topics = KafkaTopics.MEMBER_SUSPENDED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "memberSuspendedListenerFactory"
    )
    @Transactional
    public void handle(
            @Payload MemberSuspendedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[Kafka] member.suspended 수신 - memberId={}, partition={}, offset={}",
                event.memberId(), partition, offset);

        refreshTokenRepository.revokeAllByMemberId(event.memberId(), LocalDateTime.now());

        log.info("[Kafka] Refresh Token 강제 폐기 완료 - memberId={}", event.memberId());
    }
}
