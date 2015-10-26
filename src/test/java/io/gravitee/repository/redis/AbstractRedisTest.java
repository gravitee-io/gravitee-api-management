package io.gravitee.repository.redis;

import io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public abstract class AbstractRedisTest {

    @Configuration
    @Import({RateLimitRepositoryConfiguration.class})
    static class ContextConfiguration {

    }
}
