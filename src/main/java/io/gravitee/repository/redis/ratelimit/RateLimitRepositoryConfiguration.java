package io.gravitee.repository.redis.ratelimit;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
public class RateLimitRepositoryConfiguration {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new RedisConnectionFactory();
    }

    @Bean
    public RedisTemplate redisTemplate(org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public RateLimitRepository rateLimitRepository() {
        return new RedisRateLimitRepository();
    }
}
