package com.techsync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techsync.dto.AlarmEvent;
import com.techsync.service.AlarmService;
import com.techsync.service.AlarmServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * Redis "alarm" 채널을 구독하여, 수신한 이벤트를 이 인스턴스의 SSE emitter 로 전달한다.
 * 다중 인스턴스 환경에서 어느 서버가 발행하든 모든 서버가 자기 연결 사용자에게 전달할 수 있다.
 */
@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisSubscriberConfig.class);

    private final AlarmService alarmService;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer alarmListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(alarmMessageListener(), new ChannelTopic(AlarmServiceImpl.ALARM_CHANNEL));
        return container;
    }

    private MessageListener alarmMessageListener() {
        return (message, pattern) -> {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                AlarmEvent event = objectMapper.readValue(body, AlarmEvent.class);
                alarmService.dispatchToLocalEmitters(event);
            } catch (Exception e) {
                log.warn("알림 이벤트 처리 실패: {}", e.getMessage());
            }
        };
    }
}
