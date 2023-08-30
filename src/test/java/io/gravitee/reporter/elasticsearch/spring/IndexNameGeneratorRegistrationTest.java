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
package io.gravitee.reporter.elasticsearch.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.elasticsearch.ElasticsearchReporter;
import io.gravitee.reporter.elasticsearch.ElasticsearchReporterTest;
import io.gravitee.reporter.elasticsearch.IntegrationTestConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.name.IndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.MultiTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeIndexNameGenerator;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ElasticsearchReporterTest.TestConfig.class })
public class IndexNameGeneratorRegistrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ReporterConfiguration configuration;

    @Autowired
    ElasticsearchReporter reporter;

    @Autowired
    ConfigurableEnvironment environment;

    @Value("${elasticsearch.version:" + IntegrationTestConfiguration.ELASTICSEARCH_DEFAULT_VERSION + "}")
    private String elasticsearchVersion;

    private static final long TIMESTAMP = Instant.parse("2021-12-13T00:00:00.00Z").toEpochMilli();

    @AfterEach
    public void tearDown() throws Exception {
        reporter.stop();
    }

    @Test
    public void testIndexNameGeneratorRegistration_should_register_per_type_and_date_generator() throws Exception {
        if (elasticsearchVersion.startsWith("5")) {
            configuration.setPerTypeIndex(true);
        }
        if (elasticsearchVersion.startsWith("6")) {
            configuration.setIndexMode("daily");
        }
        if (elasticsearchVersion.startsWith("7")) {
            configuration.setIndexMode("daily");
        }
        if (elasticsearchVersion.startsWith("8")) {
            configuration.setIndexMode("daily");
        }
        reporter.start();
        IndexNameGenerator indexNameGenerator = (IndexNameGenerator) applicationContext.getBean("indexNameGenerator");
        assertThat(indexNameGenerator.getClass()).isEqualTo(PerTypeAndDateIndexNameGenerator.class);

        String generatedName = indexNameGenerator.generate(new Log(TIMESTAMP));
        assertThat(generatedName).isEqualTo("gravitee-log-2021.12.13");
    }

    @Test
    public void testIndexNameGenerator_should_register_multi_type_or_per_type_generator() throws Exception {
        if (elasticsearchVersion.startsWith("5")) {
            configuration.setPerTypeIndex(false);
        }
        if (elasticsearchVersion.startsWith("6")) {
            configuration.setIndexMode("ilm");
        }
        if (elasticsearchVersion.startsWith("7")) {
            configuration.setIndexMode("ilm");
        }
        if (elasticsearchVersion.startsWith("8")) {
            configuration.setIndexMode("ilm");
        }
        reporter.start();

        IndexNameGenerator indexNameGenerator = (IndexNameGenerator) applicationContext.getBean("indexNameGenerator");
        String generatedName = indexNameGenerator.generate(new Log(TIMESTAMP));

        if (elasticsearchVersion.startsWith("5")) {
            assertThat(indexNameGenerator.getClass()).isEqualTo(MultiTypeIndexNameGenerator.class);
            assertThat(generatedName).isEqualTo("gravitee-2021.12.13");
        }
        if (elasticsearchVersion.startsWith("6")) {
            assertThat(indexNameGenerator.getClass()).isEqualTo(PerTypeIndexNameGenerator.class);
            assertThat(generatedName).isEqualTo("gravitee-log");
        }
        if (elasticsearchVersion.startsWith("7")) {
            assertThat(indexNameGenerator.getClass()).isEqualTo(PerTypeIndexNameGenerator.class);
            assertThat(generatedName).isEqualTo("gravitee-log");
        }
        if (elasticsearchVersion.startsWith("8")) {
            assertThat(indexNameGenerator.getClass()).isEqualTo(PerTypeIndexNameGenerator.class);
            assertThat(generatedName).isEqualTo("gravitee-log");
        }
    }
}
