package com.community.auth.global.kafka.consumer;

import com.community.auth.global.kafka.KafkaTopics;
import com.community.auth.global.kafka.event.MemberCreatedEvent;
import com.community.auth.repository.AuthCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Member Service → Auth Service
 * 토픽: member.created
 *
 * 회원가입 완료 이벤트 수신.
 * auth_credential은 회원가입 API(/api/members/signup)에서 직접 생성하므로
 * 여기서는 중복 생성 방지를 위한 검증 로그만 남긴다.
 * Test-1
 *
 * 만약 Auth Service가 독립적으로 credential을 생성하는 구조로 변경될 경우
 * 이 Consumer에서 AuthCredential을 직접 생성하도록 확장한다.
 */
@Component
public class MemberCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MemberCreatedEventConsumer.class);

    private final AuthCredentialRepository credentialRepository;

    public MemberCreatedEventConsumer(AuthCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @KafkaListener(
            topics = KafkaTopics.MEMBER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "memberCreatedListenerFactory"
    )
    public void handle(
            @Payload MemberCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[Kafka] member.created 수신 - memberId={}, username={}, partition={}, offset={}",
                event.memberId(), event.username(), partition, offset);

        boolean exists = credentialRepository.existsByMemberId(event.memberId());
        if (exists) {
            log.info("[Kafka] credential 이미 존재 - memberId={}", event.memberId());
        } else {
            log.warn("[Kafka] credential 미존재 - memberId={}. 회원가입 API를 통해 생성되었는지 확인 필요.", event.memberId());
        }
    }
}
