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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.impl.validation.ListenerValidationServiceImpl;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiListenersTest extends ApiResourceTest {

    private static final String MERGE_PATCH_TYPE = "application/merge-patch+json";
    private static final String JSON_PATCH_TYPE = "application/json-patch+json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    UpdateApiDomainService updateApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void setUpApiAndPrimaryOwner() {
        givenApiWithListeners(List.of(defaultHttpListener("/http")));

        var apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setUpdatedAt(Date.from(Instant.ofEpochMilli(1000)));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            apiEntity
        );

        roleQueryService.resetSystemRoles(ORGANIZATION);
        primaryOwnerDomainService.initWith(
            List.of(Map.entry(API, PrimaryOwnerEntity.builder().id(USER_NAME).type(PrimaryOwnerEntity.Type.USER).build()))
        );
        membershipQueryServiceInMemory.initWith(
            List.of(
                Membership.builder()
                    .memberId(USER_NAME)
                    .referenceId(API)
                    .roleId("api-po-id-" + ORGANIZATION)
                    .referenceType(Membership.ReferenceType.API)
                    .memberType(Membership.Type.USER)
                    .build()
            )
        );

        doAnswer(inv -> inv.getArgument(0))
            .when(updateApiDomainService)
            .updateV4(any(), any());
        doAnswer(inv -> inv.getArgument(0))
            .when(updateApiDomainService)
            .validateV4(any(), any());
    }

    @AfterEach
    public void tearDown() {
        Stream.of(apiCrudService, membershipQueryServiceInMemory, primaryOwnerDomainService).forEach(InMemoryAlternative::reset);
        reset(updateApiDomainService, apiSearchServiceV4);
    }

    private void givenApiWithListeners(List<Listener> listeners) {
        var base = ApiFixtures.aProxyApiV4();
        var api = base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().listeners(listeners).build()).build();
        apiCrudService.initWith(List.of(api));
    }

    private static HttpListener defaultHttpListener(String path) {
        return HttpListener.builder()
            .paths(List.of(Path.builder().path(path).build()))
            .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
            .build();
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> uppercaseListenerMap(String typeUppercase, String path) {
        return Map.of(
            "type",
            typeUppercase,
            "paths",
            List.of(Map.of("path", path)),
            "entrypoints",
            List.of(Map.of("type", "http-proxy", "configuration", "{}"))
        );
    }

    private static Map<String, Object> lowercaseListenerMap(String typeLowercase, String path) {
        return Map.of(
            "type",
            typeLowercase,
            "paths",
            List.of(Map.of("path", path)),
            "entrypoints",
            List.of(Map.of("type", "http-proxy", "configuration", "{}"))
        );
    }

    private ApiV4 patchAndAssertOk(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));
        return assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
    }

    @ParameterizedTest
    @ValueSource(strings = { "HTTP", "TCP", "SUBSCRIPTION" })
    void patch_listener_type_uppercase_returns_200_and_persists(String uppercaseType) {
        givenApiWithListeners(List.of(defaultHttpListener("/existing")));

        var listeners = List.of(uppercaseListenerMap(uppercaseType, "/patched"));
        var body = "{\"listeners\":" + toJson(listeners) + "}";

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);

        if ("HTTP".equals(uppercaseType)) {
            var persistedListeners = capturePersistedListeners();
            Assertions.assertThat(persistedListeners).hasSize(1);
            Assertions.assertThat(persistedListeners.getFirst().getType()).isEqualTo(ListenerType.HTTP);
            Assertions.assertThat(((HttpListener) persistedListeners.getFirst()).getPaths())
                .extracting(Path::getPath)
                .containsExactly("/patched");
        }
    }

    @Test
    void patch_listeners_round_trip_from_get_response_returns_200() {
        givenApiWithListeners(List.of(defaultHttpListener("/http")));

        var listeners = List.of(uppercaseListenerMap("HTTP", "/http"));
        var body = "{\"listeners\":" + toJson(listeners) + "}";

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);

        var persistedListeners = capturePersistedListeners();
        Assertions.assertThat(persistedListeners).hasSize(1);
        Assertions.assertThat(persistedListeners.getFirst().getType()).isEqualTo(ListenerType.HTTP);
        Assertions.assertThat(((HttpListener) persistedListeners.getFirst()).getPaths()).extracting(Path::getPath).containsExactly("/http");
    }

    @Test
    void merge_patch_omitting_listeners_preserves_previous_listeners() {
        givenApiWithListeners(List.of(defaultHttpListener("/existing")));

        var body = "{\"name\":\"updated-name\"}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getListeners()).isNotEmpty();

        var persistedListeners = capturePersistedListeners();
        Assertions.assertThat(persistedListeners).hasSize(1);
        Assertions.assertThat(persistedListeners.getFirst()).isInstanceOf(HttpListener.class);
        Assertions.assertThat(((HttpListener) persistedListeners.getFirst()).getPaths())
            .extracting(Path::getPath)
            .containsExactly("/existing");
    }

    private List<Listener> capturePersistedListeners() {
        var captor = ArgumentCaptor.forClass(Api.class);
        Mockito.verify(updateApiDomainService).updateV4(captor.capture(), any());
        return captor.getValue().getApiDefinitionHttpV4().getListeners();
    }

    @Test
    void merge_patch_listeners_null_is_a_no_op() {
        givenApiWithListeners(List.of(defaultHttpListener("/existing")));

        var body = "{\"listeners\":null}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void json_patch_remove_listeners_is_a_no_op() {
        givenApiWithListeners(List.of(defaultHttpListener("/existing")));

        var body = "[{\"op\":\"remove\",\"path\":\"/listeners\"}]";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, JSON_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void merge_patch_with_empty_listeners_array_returns_400() {
        var listenerValidator = new ListenerValidationServiceImpl(
            mock(VerifyApiPathDomainService.class),
            mock(EntrypointConnectorPluginService.class),
            mock(EndpointConnectorPluginService.class),
            mock(CorsValidationService.class),
            mock(VerifyApiHostsDomainService.class)
        );
        doAnswer(inv -> {
            var api = (Api) inv.getArgument(0);
            var entity = ApiAdapter.INSTANCE.toUpdateApiEntity(api, api.getApiDefinitionHttpV4());
            listenerValidator.validateAndSanitizeHttpV4(null, api.getId(), entity.getListeners(), entity.getEndpointGroups());
            return api;
        })
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var body = "{\"listeners\":[]}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
        Assertions.assertThat(error.getTechnicalCode()).isEqualTo("listeners.missing");
    }

    @ParameterizedTest
    @ValueSource(strings = { "http", "tcp", "subscription", "kafka" })
    void patch_lowercase_listener_type_returns_400(String lowercaseType) {
        givenApiWithListeners(List.of(defaultHttpListener("/existing")));

        var listeners = List.of(lowercaseListenerMap(lowercaseType, "/patched"));
        var body = "{\"listeners\":" + toJson(listeners) + "}";

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }
}
