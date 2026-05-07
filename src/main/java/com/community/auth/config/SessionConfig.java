package com.community.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * HttpSession 을 Redis 로 위임하여 여러 인스턴스 간 세션 공유를 가능하게 한다.
 */
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
}

