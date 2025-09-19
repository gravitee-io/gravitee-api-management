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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.exceptions.InvalidLicenseException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
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
    private Api repositoryApi;

    @Mock
    private io.gravitee.definition.model.v4.Api apiV4;

    @Mock
    private io.gravitee.definition.model.v4.nativeapi.NativeApi nativeApiV4;

    @Mock
    private io.gravitee.definition.model.Api apiV2;

    @Mock
    private License license;

    @Mock
    private ObjectMapper objectMapper;

    private ApiLicenseService apiLicenseService;

    @Before
    public void init() throws Exception {
        openMocks(this);

        when(apiSearchService.findRepositoryApiById(executionContext, API)).thenReturn(repositoryApi);
        when(repositoryApi.getType()).thenReturn(ApiType.PROXY);
        when(repositoryApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(repositoryApi.getDefinition()).thenReturn("dummy definition");

        when(objectMapper.readValue("dummy definition", io.gravitee.definition.model.v4.Api.class)).thenReturn(apiV4);
        when(objectMapper.readValue("dummy definition", io.gravitee.definition.model.v4.nativeapi.NativeApi.class)).thenReturn(nativeApiV4);
        when(objectMapper.readValue("dummy definition", io.gravitee.definition.model.Api.class)).thenReturn(apiV2);

        when(apiV4.getPlugins()).thenReturn(List.of(new Plugin("apiV4-type", "apiV4-id"), new Plugin("another", "plugin")));
        when(nativeApiV4.getPlugins()).thenReturn(
            List.of(new Plugin("apiV4-kafka-type", "apiV4-kafka-id"), new Plugin("another", "plugin"))
        );
        when(apiV2.getPlugins()).thenReturn(List.of(new Plugin("apiV2-type", "apiV2-id"), new Plugin("another", "plugin")));

        lenient().when(licenseManager.getPlatformLicense()).thenReturn(license);
        apiLicenseService = new ApiLicenseServiceImpl(licenseManager, apiSearchService, objectMapper);
    }

    @Test
    public void should_verify_v4_definition() throws Exception {
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
        var v4LicensePlugins = List.of(new LicenseManager.Plugin("apiV4-type", "apiV4-id"), new LicenseManager.Plugin("another", "plugin"));
        verify(licenseManager, times(1)).validatePluginFeatures(eq(executionContext.getOrganizationId()), eq(v4LicensePlugins));
    }

    @Test
    public void should_verify_v4_native_definition() throws Exception {
        when(repositoryApi.getType()).thenReturn(ApiType.NATIVE);
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
        var v4LicensePlugins = List.of(
            new LicenseManager.Plugin("apiV4-kafka-type", "apiV4-kafka-id"),
            new LicenseManager.Plugin("another", "plugin")
        );
        verify(licenseManager, times(1)).validatePluginFeatures(eq(executionContext.getOrganizationId()), eq(v4LicensePlugins));
    }

    @Test
    public void should_verify_v2_definition()
        throws io.gravitee.node.api.license.ForbiddenFeatureException, io.gravitee.node.api.license.InvalidLicenseException {
        when(repositoryApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
        assertDoesNotThrow(() -> apiLicenseService.checkLicense(executionContext, API));
        var v2LicensePlugins = List.of(new LicenseManager.Plugin("apiV2-type", "apiV2-id"), new LicenseManager.Plugin("another", "plugin"));
        verify(licenseManager, times(1)).validatePluginFeatures(eq(executionContext.getOrganizationId()), eq(v2LicensePlugins));
    }

    @Test
    public void should_throw_invalid_license_exception()
        throws io.gravitee.node.api.license.ForbiddenFeatureException, io.gravitee.node.api.license.InvalidLicenseException {
        doThrow(new io.gravitee.node.api.license.InvalidLicenseException("invalid license"))
            .when(licenseManager)
            .validatePluginFeatures(anyString(), anyCollection());
        assertThrows(InvalidLicenseException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_throw_forbidden_feature_exception()
        throws io.gravitee.node.api.license.ForbiddenFeatureException, io.gravitee.node.api.license.InvalidLicenseException {
        var forbiddenFeature = new LicenseManager.ForbiddenFeature("feature", "plugin");
        doThrow(new io.gravitee.node.api.license.ForbiddenFeatureException(Set.of(forbiddenFeature)))
            .when(licenseManager)
            .validatePluginFeatures(anyString(), anyCollection());
        assertThrows(ForbiddenFeatureException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }

    @Test
    public void should_transform_parsing_error_to_technical_management_exception() throws JsonProcessingException {
        when(objectMapper.readValue(anyString(), any(io.gravitee.definition.model.v4.Api.class.getClass()))).thenThrow(
            JsonMappingException.class
        );
        assertThrows(TechnicalManagementException.class, () -> apiLicenseService.checkLicense(executionContext, API));
    }
}
