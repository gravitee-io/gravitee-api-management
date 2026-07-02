/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.hazelcast.ratelimit;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.FileSystemYamlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import java.io.FileNotFoundException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Owns the embedded Hazelcast instance dedicated to rate-limit storage. Independent of any
 * cluster.type=hazelcast or cache.type=hazelcast plugin: this plugin always boots its own
 * HazelcastInstance when ratelimit.type=hazelcast is selected, so operators do not need to enable
 * any other Hazelcast subsystem.
 */
@Configuration
public class RateLimitRepositoryConfiguration {

    public static final String RATE_LIMIT_MAP = "rate-limits";
    public static final String TOKEN_BUCKET_MAP = "token-buckets";

    @Value("${ratelimit.hazelcast.config-path:${gravitee.home}/config/hazelcast-ratelimit.xml}")
    private String hazelcastConfigFilePath;

    @Value("${ratelimit.hazelcast.instance-name:gio-apim-ratelimit-hz}")
    private String hazelcastInstanceName;

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance ratelimitHazelcastInstance() throws FileNotFoundException {
        Config config = fromFilePath(hazelcastConfigFilePath);
        config.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");
        config.setProperty(ClusterProperty.SHUTDOWNHOOK_ENABLED.getName(), "false");
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setInstanceName(hazelcastInstanceName);
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public RateLimitRepository<RateLimit> rateLimitRepository(HazelcastInstance ratelimitHazelcastInstance) {
        return new HazelcastRateLimitRepository(ratelimitHazelcastInstance.getMap(RATE_LIMIT_MAP));
    }

    @Bean
    public TokenBucketRateLimitRepository<TokenBucket> tokenBucketRateLimitRepository(HazelcastInstance ratelimitHazelcastInstance) {
        return new HazelcastTokenBucketRateLimitRepository(ratelimitHazelcastInstance.getMap(TOKEN_BUCKET_MAP));
    }

    private Config fromFilePath(String filePath) throws FileNotFoundException {
        String suffix = filePath.toLowerCase(Locale.ROOT);
        if (suffix.endsWith(".xml")) {
            return new FileSystemXmlConfig(filePath);
        } else if (suffix.endsWith(".yaml") || suffix.endsWith(".yml")) {
            return new FileSystemYamlConfig(filePath);
        }
        throw new IllegalArgumentException("Only xml or yaml file supported for Hazelcast rate-limit configuration");
    }
}
