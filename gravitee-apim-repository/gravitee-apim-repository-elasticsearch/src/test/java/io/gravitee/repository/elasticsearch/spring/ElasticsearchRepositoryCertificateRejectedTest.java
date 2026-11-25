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
package io.gravitee.repository.elasticsearch.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ElasticsearchRepositoryCertificateRejectedTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(ElasticsearchRepositoryConfiguration.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void should_name_the_trust_settings_when_the_cluster_certificate_is_rejected() {
        final AtomicInteger attempts = new AtomicInteger();
        final ElasticsearchInfo info = new ElasticsearchInfo();
        final Client client = mock(Client.class);
        when(client.getInfo()).thenReturn(
            Single.defer(() ->
                attempts.getAndIncrement() == 0
                    ? Single.error(new RuntimeException(new SSLHandshakeException("(certificate_unknown) PKIX path building failed")))
                    : Single.just(info)
            )
        );

        assertThat(new ElasticsearchRepositoryConfiguration().elasticsearchInfo(client)).isSameAs(info);

        assertThat(appender.list)
            .filteredOn(event -> event.getLevel() == Level.ERROR)
            .extracting(ILoggingEvent::getFormattedMessage)
            .singleElement()
            .asString()
            .contains("'analytics.elasticsearch.ssl.truststore'", "'analytics.elasticsearch.ssl.trustAll'");
    }
}
