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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class APIV1toAPIV2UpgraderTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiService apiService;

    private APIV1toAPIV2Upgrader cut;

    @Mock
    private Appender<ILoggingEvent> appender;

    @Before
    public void setUp() throws Exception {
        cut = new APIV1toAPIV2Upgrader(apiRepository, apiService);

        Logger logger = (Logger) LoggerFactory.getLogger(APIV1toAPIV2Upgrader.class);
        logger.addAppender(appender);
        reset(appender);
    }

    @Test
    public void upgrade_should_failed_because_of_exception() throws TechnicalException {
        when(apiRepository.searchV1ApisId()).thenThrow(new RuntimeException());

        assertFalse(cut.upgrade());

        verify(apiRepository, times(1)).searchV1ApisId();
        verifyNoMoreInteractions(apiRepository);
    }

    @Test
    public void should_order_equals_620() {
        assertThat(cut.getOrder()).isEqualTo(620);
    }

    @Test
    public void should_do_nothing_when_nothing_to_migrate() throws TechnicalException {
        cut.upgrade();
        verifyNoInteractions(apiService);
    }

    @Test
    public void should_migrate_v1_apis() {
        when(apiRepository.searchV1ApisId()).thenReturn(Stream.of("api1", "api2", "api3"));
        when(apiService.migrate(GraviteeContext.getExecutionContext(), "api1"))
            .thenReturn(ApiEntity.builder().updatedAt(Date.from(Instant.now().plusSeconds(30))).build());
        when(apiService.migrate(GraviteeContext.getExecutionContext(), "api2")).thenThrow(new TechnicalManagementException());
        when(apiService.migrate(GraviteeContext.getExecutionContext(), "api3"))
            .thenReturn(ApiEntity.builder().updatedAt(Date.from(Instant.now().plusSeconds(35))).build());

        cut.upgrade();

        verify(appender, times(1))
            .doAppend(
                argThat(event ->
                    Level.INFO == event.getLevel() &&
                    "2 V1 APIs have been successfully migrated to V2 APIs!".equals(event.getFormattedMessage())
                )
            );
        verify(appender, times(1))
            .doAppend(
                argThat(event ->
                    Level.INFO == event.getLevel() &&
                    "1 V1 APIs haven't been migrated to V2 APIs. Check logs for more details.".equals(event.getFormattedMessage())
                )
            );

        verify(appender, times(1))
            .doAppend(
                argThat(event ->
                    Level.ERROR == event.getLevel() && "Api api2 has not been migrated to v2".equals(event.getFormattedMessage())
                )
            );

        verify(apiService, times(3)).migrate(any(), any());
    }
}
