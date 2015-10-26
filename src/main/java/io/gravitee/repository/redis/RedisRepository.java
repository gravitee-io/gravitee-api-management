package io.gravitee.repository.redis;

import io.gravitee.repository.Repository;
import io.gravitee.repository.Scope;
import io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RedisRepository implements Repository {

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public Scope[] scopes() {
        return new Scope [] {
                Scope.RATE_LIMIT
        };
    }

    @Override
    public Class<?> configuration(Scope scope) {
        switch (scope) {
            case RATE_LIMIT:
                return RateLimitRepositoryConfiguration.class;

        }

        return null;
    }
}
