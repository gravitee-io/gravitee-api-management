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
package io.gravitee.rest.api.service.v4.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
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
    private LicenseManager licenseManager;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private License license;

    private ApiLicenseService apiLicenseService;

    @Before
    public void init() throws Exception {
        openMocks(this);
        when(apiSearchService.findGenericById(executionContext, API)).thenReturn(apiEntity);
        when(apiEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        lenient().when(licenseManager.getPlatformLicense()).thenReturn(license);
        apiLicenseService = new ApiLicenseServiceImpl(licenseManager, apiSearchService);
    }

    @Test
    public void should_not_throw_error_with_definition_version_v2() {
        when(apiEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_dummy_http_entrypoint() {
        when(apiEntity.getListeners()).thenReturn(listeners(List.of()));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_webhook_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-webhook")).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("webhook")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_webhook_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-webhook")).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("webhook")));

        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_websocket_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-websocket")).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("websocket")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_websocket_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-websocket")).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("websocket")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_sse_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-sse")).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("sse")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_sse_ntrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-sse")).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("sse")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_http_get_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-http-get")).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-get")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_http_get_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-http-get")).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-get")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_http_post_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-http-post")).thenReturn(true);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-post")));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_http_post_entrypoint() {
        when(license.isFeatureEnabled("apim-en-entrypoint-http-post")).thenReturn(false);
        when(apiEntity.getListeners()).thenReturn(listeners(entrypoints("http-post")));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_kafka_endpoint() {
        when(license.isFeatureEnabled("apim-en-endpoint-kafka")).thenReturn(true);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("kafka"));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_kafka_endpoint() {
        when(license.isFeatureEnabled("apim-en-endpoint-kafka")).thenReturn(false);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("kafka"));
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_not_throw_error_with_mqtt5_endpoint() {
        when(license.isFeatureEnabled("apim-en-endpoint-mqtt5")).thenReturn(true);
        when(apiEntity.getEndpointGroups()).thenReturn(endpointGroups("mqtt5"));
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_error_with_mqtt5_endpoint() {
        when(license.isFeatureEnabled("apim-en-endpoint-mqtt5")).thenReturn(false);
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
        HttpListener httpListener = HttpListener.builder().entrypoints(entrypoints).build();
        return List.of(httpListener);
    }
}
