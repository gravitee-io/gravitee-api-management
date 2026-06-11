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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityLogsDataPortAdapterTest {

    @Mock
    private ConnectionLogsCrudService connectionLogsCrudService;

    @Mock
    private UserContextLoader userContextLoader;

    @Mock
    private PlanCrudService planCrudService;

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private InstanceQueryService instanceQueryService;

    @Mock
    private ApiProductQueryService apiProductQueryService;

    private ObservabilityLogsDataPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObservabilityLogsDataPortAdapter(
            connectionLogsCrudService,
            userContextLoader,
            planCrudService,
            applicationCrudService,
            instanceQueryService,
            apiProductQueryService
        );
    }

    @Test
    void should_reject_filter_not_translatable_to_log_search() {
        var query = LogsSearchQuery.builder()
            .apiIds(Set.of("api-1"))
            .apiNamesById(Map.of("api-1", "API 1"))
            .conditions(List.of(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.IN, List.of("2XX"))))
            .page(1)
            .perPage(20)
            .build();

        assertThatThrownBy(() -> adapter.searchLogs("org-1", "env-1", query))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("HTTP_STATUS_CODE_GROUP");

        verifyNoInteractions(connectionLogsCrudService);
    }
}
