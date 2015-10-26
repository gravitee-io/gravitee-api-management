package io.gravitee.repository.redis.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RedisConnectionFactory implements FactoryBean<org.springframework.data.redis.connection.RedisConnectionFactory> {

    private final Logger logger = LoggerFactory.getLogger(RedisConnectionFactory.class);

    @Autowired
    private Environment environment;

    @Override
    public org.springframework.data.redis.connection.RedisConnectionFactory getObject() throws Exception {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();

        jedisConnectionFactory.setHostName("localhost");
        jedisConnectionFactory.setPort(6379);

        jedisConnectionFactory.afterPropertiesSet();

        return jedisConnectionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return org.springframework.data.redis.connection.RedisConnectionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
