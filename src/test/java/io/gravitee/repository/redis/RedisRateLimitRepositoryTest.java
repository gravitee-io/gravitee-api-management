package io.gravitee.repository.redis;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RedisRateLimitRepositoryTest extends AbstractRedisTest {

    @Autowired
    private RateLimitRepository rateLimitRepository;

    @Test
    public void test() {
        System.out.println(rateLimitRepository.acquire("test", 1, 10, 2, TimeUnit.SECONDS));
        System.out.println(rateLimitRepository.acquire("test", 1, 10, 2, TimeUnit.SECONDS));
    }
}
