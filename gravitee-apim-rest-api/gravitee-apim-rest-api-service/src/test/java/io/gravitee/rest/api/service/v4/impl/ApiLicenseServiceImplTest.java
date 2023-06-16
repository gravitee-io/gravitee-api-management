/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.rest.api.service.v4.GraviteeLicenseService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiLicenseServiceImplTest {

    private static final String API = "api";
    private static final ExecutionContext executionContext = new ExecutionContext("test", "test");

    @Mock
    private GraviteeLicenseService graviteeLicenseService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiEntity apiEntity;

    private ApiLicenseService apiLicenseService;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        when(apiSearchService.findGenericById(executionContext, API)).thenReturn(apiEntity);
        when(apiEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        apiLicenseService = new ApiLicenseServiceImpl(graviteeLicenseService, apiSearchService);
    }

    @Test
    public void shouldNotThrowErrorWithDefinitionVersionV2() {
        when(apiEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithDummyHttpEntrypoint() {
        when(apiEntity.getListeners()).thenReturn(listeners(List.of()));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithWebhookEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_WEBHOOK)).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("webhook")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithWebhookEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_WEBHOOK)).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("webhook")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithWebsocketEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_WEBSOCKET)).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("websocket")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithWebsocketEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_WEBSOCKET)).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("websocket")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithSseEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_SSE)).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("sse")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithSseEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_SSE)).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("sse")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithHttpGetEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_HTTP_GET)).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-get")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithHttpGetEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_HTTP_GET)).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-get")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithHttpPostEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_HTTP_POST)).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-post")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithHttpPostEntrypoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENTRYPOINT_HTTP_POST)).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-post")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithKafkaEndpoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENDPOINT_KAFKA)).thenReturn(false);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("kafka"));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithHKafkaEndpoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENDPOINT_KAFKA)).thenReturn(true);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("kafka"));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldNotThrowErrorWithMqttEndpoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENDPOINT_MQTT5)).thenReturn(false);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("mqtt5"));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void shouldThrowErrorWithHMqttEndpoint() {
        when(graviteeLicenseService.isFeatureMissing(FEATURE_ENDPOINT_MQTT5)).thenReturn(true);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("mqtt5"));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    private static List<EndpointGroup> endpointGroups(String endpointType) {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setType(endpointType);
        return List.of(endpointGroup);
    }

    private static List<Entrypoint> entrypoints(String type) {
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType(type);
        return List.of(entrypoint);
    }

    private static List<Listener> listeners(List<Entrypoint> entrypoints) {
        HttpListener httpListener = new HttpListener();
        httpListener.setEntrypoints(entrypoints);
        return List.of(httpListener);
    }
}
