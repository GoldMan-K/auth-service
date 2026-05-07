package com.community.auth.global.kafka.producer;

import com.community.auth.global.kafka.KafkaTopics;
import com.community.auth.global.kafka.event.MemberLoginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Auth Service 도메인 이벤트 → Kafka 발행
 */
@Component
public class AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuthEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * auth.member.login 이벤트 발행
     * 로그인 성공 시 호출 → Member Service가 lastLoginAt / lastLoginIp 갱신
     *
     * @param memberId 로그인한 회원 ID
     * @param loginIp  로그인 요청 IP
     */
    public void publishMemberLogin(Long memberId, String loginIp) {
        MemberLoginEvent event = MemberLoginEvent.of(memberId, loginIp);
        String key = String.valueOf(memberId);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.AUTH_MEMBER_LOGIN, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] auth.member.login 발행 실패 - memberId={}, error={}", memberId, ex.getMessage(), ex);
            } else {
                log.info("[Kafka] auth.member.login 발행 완료 - memberId={}, offset={}",
                        memberId, result.getRecordMetadata().offset());
            }
        });
    }
}
