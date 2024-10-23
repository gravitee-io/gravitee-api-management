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
import io.gravitee.apim.core.api.domain_service.GroupValidationService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PrimaryOwnerAdapter;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidateApiDomainServiceLegacyWrapperTest {

    private static final PrimaryOwnerEntity PRIMARY_OWNER = PrimaryOwnerEntity
        .builder()
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

    @Mock
    TagsValidationService tagsValidationService;

    @Mock
    GroupValidationService groupValidationService;

    @Mock
    ListenerValidationService listenerValidationService;

    @Mock
    EndpointGroupsValidationService endpointGroupsValidationService;

    @InjectMocks
    ValidateApiDomainServiceLegacyWrapper service;

    @Nested
    class ApiHttpV4 {

        @Test
        void should_call_legacy_service() {
            Api api = ApiFixtures.aProxyApiV4();
            service.validateAndSanitizeForCreation(api, PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID);

            verify(apiValidationService)
                .validateAndSanitizeNewApi(
                    eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
                    eq(ApiAdapter.INSTANCE.toNewApiEntity(api)),
                    eq(PrimaryOwnerAdapter.INSTANCE.toRestEntity(PRIMARY_OWNER))
                );
        }

        @Test
        void should_update_api_with_sanitized_value() {
            var api = ApiFixtures
                .aProxyApiV4()
                .toBuilder()
                .apiDefinitionHttpV4(
                    ApiFixtures
                        .aProxyApiV4()
                        .getApiDefinitionHttpV4()
                        .toBuilder()
                        .services(
                            ApiServices
                                .builder()
                                .dynamicProperty(Service.builder().configuration("configuration-to-sanitize").build())
                                .build()
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
                .validateAndSanitizeHttpV4(any(), any());

            var result = service.validateAndSanitizeForCreation(api, PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID);

            CoreAssertions
                .assertThat(result)
                .hasName("sanitized")
                .hasVersion("sanitized")
                .hasType(ApiType.MESSAGE)
                .hasDescription("sanitized")
                .hasOnlyGroups(Set.of("sanitized"))
                .hasOnlyTags(Set.of("sanitized"));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getApiDefinitionHttpV4().getListeners()).isEmpty();
                soft.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).isEmpty();
                soft.assertThat(result.getApiDefinitionHttpV4().getAnalytics()).isEqualTo(Analytics.builder().enabled(true).build());
                soft.assertThat(result.getApiDefinitionHttpV4().getFlows()).containsExactly(FlowFixtures.aSimpleFlowV4());
                soft.assertThat(result.getApiDefinitionHttpV4().getFlowExecution()).isNull();
                soft
                    .assertThat(result.getApiDefinitionHttpV4().getServices().getDynamicProperty().getConfiguration())
                    .isEqualTo("sanitized");
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

    @Nested
    class ApiNativeV4 {

        @Test
        void should_call_legacy_service() {
            Api api = ApiFixtures.aNativeApi();
            service.validateAndSanitizeForCreation(api, PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID);

            verify(apiValidationService).validateDynamicProperties(any());
        }

        @Test
        void should_update_api_with_sanitized_value() {
            var api = ApiFixtures
                .aNativeApi()
                .toBuilder()
                .description("sani<img src=\"../../../image.png\">tized")
                .apiDefinitionNativeV4(
                    ApiFixtures
                        .aNativeApi()
                        .getApiDefinitionNativeV4()
                        .toBuilder()
                        .services(
                            NativeApiServices
                                .builder()
                                .dynamicProperty(Service.builder().configuration("configuration-to-sanitize").build())
                                .build()
                        )
                        .build()
                )
                .build();

            doAnswer(invocation -> Set.of("sanitized")).when(tagsValidationService).validateAndSanitize(any(), any(), any());

            doAnswer(invocation -> Set.of("sanitized")).when(groupValidationService).validateAndSanitize(any(), any(), any());

            doAnswer(invocation ->
                    List.of(KafkaListener.builder().entrypoints(List.of(NativeEntrypoint.builder().type("sanitized").build())).build())
                )
                .when(listenerValidationService)
                .validateAndSanitizeNativeV4(any(), any(), any(), any());

            doAnswer(invocation ->
                    List.of(
                        NativeEndpointGroup
                            .builder()
                            .type("sanitized")
                            .endpoints(List.of(NativeEndpoint.builder().name("sanitized").type("sanitized").build()))
                            .build()
                    )
                )
                .when(endpointGroupsValidationService)
                .validateAndSanitizeNativeV4(any());

            doAnswer(invocation -> {
                    Service dynamicProperties = invocation.getArgument(0);
                    dynamicProperties.setConfiguration("sanitized");
                    return null;
                })
                .when(apiValidationService)
                .validateDynamicProperties(any());

            doAnswer(invocation -> List.of(FlowFixtures.aNativeFlowV4()))
                .when(flowValidationDomainService)
                .validateAndSanitizeNativeV4(any());

            var result = service.validateAndSanitizeForCreation(api, PRIMARY_OWNER, ENVIRONMENT_ID, ORGANIZATION_ID);

            CoreAssertions
                .assertThat(result)
                .hasName(api.getName())
                .hasVersion(api.getVersion())
                .hasType(ApiType.NATIVE)
                .hasDescription("sanitized")
                .hasOnlyGroups(Set.of("sanitized"))
                .hasOnlyTags(Set.of("sanitized"));

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(result.getApiDefinitionNativeV4().getListeners())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(KafkaListener.class)
                    .hasFieldOrPropertyWithValue("type", ListenerType.KAFKA)
                    .extracting(l -> l.getEntrypoints().get(0))
                    .hasFieldOrPropertyWithValue("type", "sanitized");
                soft
                    .assertThat(result.getApiDefinitionNativeV4().getEndpointGroups())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(NativeEndpointGroup.class)
                    .hasFieldOrPropertyWithValue("type", "sanitized")
                    .extracting(ls -> ls.getEndpoints().get(0))
                    .isInstanceOf(NativeEndpoint.class)
                    .hasFieldOrPropertyWithValue("name", "sanitized")
                    .hasFieldOrPropertyWithValue("type", "sanitized");
                soft.assertThat(result.getApiDefinitionNativeV4().getFlows()).containsExactly(FlowFixtures.aNativeFlowV4());
                soft
                    .assertThat(result.getApiDefinitionNativeV4().getServices().getDynamicProperty().getConfiguration())
                    .isEqualTo("sanitized");
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
}
