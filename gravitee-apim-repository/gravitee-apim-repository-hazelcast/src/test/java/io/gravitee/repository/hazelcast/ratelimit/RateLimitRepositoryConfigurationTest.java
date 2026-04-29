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

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.core.HazelcastInstance;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RateLimitRepositoryConfigurationTest {

    private static final String MINIMAL_HZ_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <hazelcast xmlns="http://www.hazelcast.com/schema/config"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.hazelcast.com/schema/config
                   http://www.hazelcast.com/schema/config/hazelcast-config-5.3.xsd">
            <cluster-name>graviteeio-apim-ratelimit-spring-test</cluster-name>
            <network>
                <port auto-increment="true" port-count="100">5901</port>
                <join>
                    <auto-detection enabled="false"/>
                    <multicast enabled="false"/>
                    <tcp-ip enabled="false"/>
                </join>
            </network>
        </hazelcast>
        """;

    @Test
    void wires_HazelcastInstance_and_RateLimitRepository_beans(@TempDir Path tmp) throws IOException {
        Path xml = tmp.resolve("hazelcast-ratelimit.xml");
        Files.writeString(xml, MINIMAL_HZ_XML);

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            String instanceName = "test-config-" + System.nanoTime();
            ctx
                .getEnvironment()
                .getPropertySources()
                .addFirst(
                    new MapPropertySource(
                        "test",
                        Map.of("ratelimit.hazelcast.config-path", xml.toString(), "ratelimit.hazelcast.instance-name", instanceName)
                    )
                );
            ctx.register(RateLimitRepositoryConfiguration.class);
            ctx.refresh();

            HazelcastInstance hz = ctx.getBean(HazelcastInstance.class);
            assertThat(hz.getName()).isEqualTo(instanceName);
            assertThat(hz.getMap(RateLimitRepositoryConfiguration.RATE_LIMIT_MAP).getName()).isEqualTo(
                RateLimitRepositoryConfiguration.RATE_LIMIT_MAP
            );

            @SuppressWarnings("unchecked")
            RateLimitRepository<RateLimit> repo = (RateLimitRepository<RateLimit>) ctx.getBean(RateLimitRepository.class);
            assertThat(repo).isInstanceOf(HazelcastRateLimitRepository.class);
        }
    }
}
