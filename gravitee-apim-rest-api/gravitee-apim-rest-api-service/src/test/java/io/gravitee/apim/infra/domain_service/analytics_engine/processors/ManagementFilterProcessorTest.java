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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.api.model.Api;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ManagementFilterProcessorTest extends AbstractFilterProcessor {

    static final String API_ID1 = "api-id1";
    static final String API_ID2 = "api-id2";

    static final String API_NAME1 = "api1";
    static final String API_NAME2 = "api2";

    List<Api> apis = List.of(Api.builder().id(API_ID1).name(API_NAME1).build(), Api.builder().id(API_ID2).name(API_NAME2).build());

    List<String> apiIds = List.of(API_ID1, API_ID2);

    MetricsContext metricsContext;

    private final FilterPreProcessor managementFilterPreProcessor = new ManagementFilterPreProcessor();

    @BeforeEach
    void setUp() {
        metricsContext = new MetricsContext(auditInfo).withApis(apis);
    }

    @Test
    public void should_return_allowed_apis() {
        var filters = managementFilterPreProcessor.buildFilters(metricsContext, List.of());

        assertThat(filters).size().isEqualTo(1);

        var value = filters.getFirst().value();
        assertThat(value).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactlyInAnyOrderElementsOf(apiIds);
    }
}
