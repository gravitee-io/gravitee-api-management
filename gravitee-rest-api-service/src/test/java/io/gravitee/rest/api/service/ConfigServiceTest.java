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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.PortalConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.impl.ConfigServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.gravitee.rest.api.model.parameters.Key.COMPANY_NAME;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceTest {

    @InjectMocks
    private ConfigServiceImpl configService = new ConfigServiceImpl();

    @Mock
    private ParameterService mockParameterService;
    @Mock
    private ReCaptchaService reCaptchaService;

    @Mock
    private ConfigurableEnvironment environment;
    @Mock
    private NewsletterService newsletterService;

    @Test
    public void shouldGetPortalConfig() {

        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.AUTHENTICATION_OAUTH2_SCOPE.key(), Arrays.asList("scope1", "scope2"));
        params.put(Key.SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));
        params.put(Key.API_LABELS_DICTIONARY.key(), Arrays.asList("label1", "label2"));

        when(mockParameterService.findAll(any(List.class))).thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        PortalConfigEntity portalConfig = configService.getPortalConfig();

        assertNotNull(portalConfig);
        assertEquals("company name", "ACME", portalConfig.getCompany().getName());
        assertEquals("force login", true, portalConfig.getAuthentication().getForceLogin().isEnabled());
        assertEquals("scopes", 2, portalConfig.getAuthentication().getOauth2().getScope().size());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalConfig.getPortal().getAnalytics().isEnabled());
        assertEquals("recaptcha siteKey", "my-site-key", portalConfig.getReCaptcha().getSiteKey());
        assertEquals("recaptcha enabled", Boolean.TRUE, portalConfig.getReCaptcha().getEnabled());
        assertEquals("plan security keyless", Boolean.TRUE, portalConfig.getPlan().getSecurity().getKeyless().isEnabled());
        assertEquals("open api swagger enabled", Boolean.TRUE, portalConfig.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled());
        assertEquals("open api swagger default", "Swagger", portalConfig.getOpenAPIDocViewer().getOpenAPIDocType().getDefaultType());
        assertEquals("api labels", 2, portalConfig.getApi().getLabelsDictionary().size());
    }

    @Test
    public void shouldGetPortalConfigFromEnvVar() {

        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.AUTHENTICATION_OAUTH2_SCOPE.key(), Arrays.asList("scope1", "scope2"));
        params.put(Key.API_LABELS_DICTIONARY.key(), Arrays.asList("label1"));
        params.put(Key.SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));

        when(mockParameterService.findAll(any(List.class))).thenReturn(params);

        when(environment.containsProperty(eq(COMPANY_NAME.key()))).thenReturn(true);
        when(environment.containsProperty(eq(Key.AUTHENTICATION_FORCELOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(Key.AUTHENTICATION_OAUTH2_SCOPE.key())).thenReturn(true);
        when(environment.containsProperty(Key.API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.containsProperty(Key.SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);
        when(environment.containsProperty(Key.PORTAL_ANALYTICS_ENABLED.key())).thenReturn(true);
        when(environment.containsProperty(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key())).thenReturn(true);

        PortalConfigEntity portalConfig = configService.getPortalConfig();

        assertNotNull(portalConfig);
        assertEquals("company name", "ACME", portalConfig.getCompany().getName());
        assertEquals("force login", true, portalConfig.getAuthentication().getForceLogin().isEnabled());
        assertEquals("scopes", 2, portalConfig.getAuthentication().getOauth2().getScope().size());
        assertEquals("labels dictionary", 1, portalConfig.getApi().getLabelsDictionary().size());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalConfig.getPortal().getAnalytics().isEnabled());
        assertEquals("open api swagger enabled", Boolean.TRUE, portalConfig.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled());
        List<String> readonlyMetadata = portalConfig.getMetadata().get(PortalConfigEntity.METADATA_READONLY);
        assertEquals("Config metadata size", 7, readonlyMetadata.size());
        assertTrue("Config metadata contains COMPANY_NAME", readonlyMetadata.contains(COMPANY_NAME.key()));
        assertTrue("Config metadata contains AUTHENTICATION_FORCELOGIN_ENABLED", readonlyMetadata.contains(Key.AUTHENTICATION_FORCELOGIN_ENABLED.key()));
        assertTrue("Config metadata contains AUTHENTICATION_OAUTH2_SCOPE", readonlyMetadata.contains(Key.AUTHENTICATION_OAUTH2_SCOPE.key()));
        assertTrue("Config metadata contains API_LABELS_DICTIONARY", readonlyMetadata.contains(Key.API_LABELS_DICTIONARY.key()));
        assertTrue("Config metadata contains SCHEDULER_NOTIFICATIONS", readonlyMetadata.contains(Key.SCHEDULER_NOTIFICATIONS.key()));
        assertTrue("Config metadata contains PORTAL_ANALYTICS_ENABLED", readonlyMetadata.contains(Key.PORTAL_ANALYTICS_ENABLED.key()));
        assertTrue("Config metadata contains OPEN_API_DOC_TYPE_SWAGGER_ENABLED", readonlyMetadata.contains(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key()));
    }

    @Test
    public void shouldCreatePortalConfig() {
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        portalConfigEntity.getCompany().setName("ACME");
        when(mockParameterService.save(COMPANY_NAME, "ACME")).thenReturn(new Parameter());

        configService.save(portalConfigEntity);

        verify(mockParameterService, times(1)).save(COMPANY_NAME, "ACME");
    }
}
