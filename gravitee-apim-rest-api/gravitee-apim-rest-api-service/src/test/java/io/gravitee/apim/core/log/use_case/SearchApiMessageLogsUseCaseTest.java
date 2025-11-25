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
package io.gravitee.apim.core.log.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.MessageLogsCrudServiceInMemory;
import io.gravitee.apim.core.log.model.MessageMetrics;
import io.gravitee.rest.api.model.analytics.SearchMessageLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiMessageLogsUseCaseTest {

    MessageLogsCrudServiceInMemory messageLogsCrudService;

    SearchApiMessageLogsUseCase underTest;

    @BeforeEach
    void setup() {
        messageLogsCrudService = new MessageLogsCrudServiceInMemory();
        messageLogsCrudService.initWith(List.of(MessageMetrics.builder().apiId("api-id").build()));
        underTest = new SearchApiMessageLogsUseCase(messageLogsCrudService);
    }

    @Test
    void should_search_message_metrics() {
        SearchApiMessageLogsUseCase.Output output = underTest.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApiMessageLogsUseCase.Input("api-id", SearchMessageLogsFilters.builder().build(), new PageableImpl(1, 10))
        );
        assertThat(output.total()).isEqualTo(1);
        assertThat(output.data()).hasSize(1);
        assertThat(output.data())
            .first()
            .satisfies(messageMetrics -> assertThat(messageMetrics.getApiId()).isEqualTo("api-id"));
    }
}
