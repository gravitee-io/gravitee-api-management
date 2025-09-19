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
package io.gravitee.apim.infra.domain_service.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import assertions.CoreAssertions;
import fixtures.core.model.ApiFixtures;
import fixtures.definition.FlowFixtures;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PrimaryOwnerAdapter;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidateApiDomainServiceLegacyWrapperTest {

    private static final PrimaryOwnerEntity PRIMARY_OWNER = PrimaryOwnerEntity.builder()
        .id("primary-owner-id")
        .displayName("John Doe")
        .email("john.doe@example.com")
        .type(PrimaryOwnerEntity.Type.USER)
        .build();
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    @Mock
    ApiValidationService apiValidationService;

    @Mock
    CategoryDomainService categoryDomainService;

    @Mock
    FlowValidationDomainService flowValidationDomainService;

    @InjectMocks
    ValidateApiDomainServiceLegacyWrapper service;

    @Test
    void should_call_legacy_service() {
        Api api = ApiFixtures.aProxyApiV4();
        service.validateAndSanitizeForCreation(api, PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID);

        verify(apiValidationService).validateAndSanitizeNewApi(
            eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
            eq(ApiAdapter.INSTANCE.toNewApiEntity(api)),
            eq(PrimaryOwnerAdapter.INSTANCE.toRestEntity(PRIMARY_OWNER))
        );
    }

    @Test
    void should_update_api_with_sanitized_value() {
        var api = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .apiDefinitionV4(
                ApiFixtures.aProxyApiV4()
                    .getApiDefinitionV4()
                    .toBuilder()
                    .services(
                        ApiServices.builder().dynamicProperty(Service.builder().configuration("configuration-to-sanitize").build()).build()
                    )
                    .build()
            )
            .build();
        doAnswer(invocation -> {
            NewApiEntity entity = invocation.getArgument(1);
            entity.setName("sanitized");
            entity.setApiVersion("sanitized");
            entity.setType(ApiType.MESSAGE);
            entity.setDescription("sanitized");
            entity.setGroups(Set.of("sanitized"));
            entity.setTags(Set.of("sanitized"));
            entity.setListeners(List.of());
            entity.setEndpointGroups(List.of());
            entity.setAnalytics(Analytics.builder().enabled(true).build());
            entity.setFlowExecution(null);
            entity.setFlows(List.of(FlowFixtures.aSimpleFlowV4()));

            return null;
        })
            .when(apiValidationService)
            .validateAndSanitizeNewApi(any(), any(), any());

        doAnswer(invocation -> {
            Service dynamicProperties = invocation.getArgument(0);
            dynamicProperties.setConfiguration("sanitized");
            return null;
        })
            .when(apiValidationService)
            .validateDynamicProperties(any());

        doAnswer(invocation -> invocation.<List<Flow>>getArgument(1))
            .when(flowValidationDomainService)
            .validateAndSanitize(any(), any());

        var result = service.validateAndSanitizeForCreation(api, PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID);

        CoreAssertions.assertThat(result)
            .hasName("sanitized")
            .hasVersion("sanitized")
            .hasType(ApiType.MESSAGE)
            .hasDescription("sanitized")
            .hasOnlyGroups(Set.of("sanitized"))
            .hasOnlyTags(Set.of("sanitized"));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getApiDefinitionV4().getListeners()).isEmpty();
            soft.assertThat(result.getApiDefinitionV4().getEndpointGroups()).isEmpty();
            soft.assertThat(result.getApiDefinitionV4().getAnalytics()).isEqualTo(Analytics.builder().enabled(true).build());
            soft.assertThat(result.getApiDefinitionV4().getFlows()).containsExactly(FlowFixtures.aSimpleFlowV4());
            soft.assertThat(result.getApiDefinitionV4().getFlowExecution()).isNull();
            soft.assertThat(result.getApiDefinitionV4().getServices().getDynamicProperty().getConfiguration()).isEqualTo("sanitized");
        });
    }

    @Test
    void should_throw_when_validation_fails() {
        doThrow(new RuntimeException("error")).when(apiValidationService).validateAndSanitizeNewApi(any(), any(), any());

        var throwable = Assertions.catchThrowable(() ->
            service.validateAndSanitizeForCreation(ApiFixtures.aProxyApiV4(), PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID)
        );

        Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
    }
}
