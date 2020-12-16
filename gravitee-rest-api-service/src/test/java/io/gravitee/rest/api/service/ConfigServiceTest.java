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
import io.gravitee.rest.api.model.parameters.ConsoleConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.PortalConfigEntity;
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

import static io.gravitee.rest.api.model.parameters.Key.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
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
        params.put(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));
        params.put(Key.API_LABELS_DICTIONARY.key(), Arrays.asList("label1", "label2"));

        when(mockParameterService.findAll(any(List.class), eq("DEFAULT"), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        PortalConfigEntity portalConfig = configService.getPortalConfig();

        assertNotNull(portalConfig);
        assertEquals("force login", true, portalConfig.getAuthentication().getForceLogin().isEnabled());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalConfig.getPortal().getAnalytics().isEnabled());
        assertEquals("recaptcha siteKey", "my-site-key", portalConfig.getReCaptcha().getSiteKey());
        assertEquals("recaptcha enabled", Boolean.TRUE, portalConfig.getReCaptcha().getEnabled());
        assertEquals("plan security keyless", Boolean.TRUE, portalConfig.getPlan().getSecurity().getKeyless().isEnabled());
        assertEquals("open api swagger enabled", Boolean.TRUE, portalConfig.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled());
        assertEquals("open api swagger default", "Swagger", portalConfig.getOpenAPIDocViewer().getOpenAPIDocType().getDefaultType());
        assertEquals("api labels", 2, portalConfig.getApi().getLabelsDictionary().size());
        assertEquals("cors exposed headers", 2, portalConfig.getCors().getExposedHeaders().size());
    }

    @Test
    public void shouldGetPortalConfigFromEnvVar() {

        Map<String, List<String>> params = new HashMap<>();
        params.put(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.API_LABELS_DICTIONARY.key(), Arrays.asList("label1"));
        params.put(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));
        params.put(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS.key(), singletonList("OnlyOneHeader"));

        when(mockParameterService.findAll(any(List.class), eq("DEFAULT"), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(params);

        when(environment.containsProperty(eq(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(Key.API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.containsProperty(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);
        when(environment.containsProperty(Key.PORTAL_ANALYTICS_ENABLED.key())).thenReturn(true);
        when(environment.containsProperty(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key())).thenReturn(true);

        PortalConfigEntity portalConfig = configService.getPortalConfig();

        assertNotNull(portalConfig);
        assertEquals("force login", true, portalConfig.getAuthentication().getForceLogin().isEnabled());
        assertEquals("labels dictionary", 1, portalConfig.getApi().getLabelsDictionary().size());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalConfig.getPortal().getAnalytics().isEnabled());
        assertEquals("open api swagger enabled", Boolean.TRUE, portalConfig.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled());
        assertEquals("cors exposed headers", 1, portalConfig.getCors().getExposedHeaders().size());
        List<String> readonlyMetadata = portalConfig.getMetadata().get(PortalConfigEntity.METADATA_READONLY);
        assertEquals("Config metadata size", 5, readonlyMetadata.size());
        assertTrue("Config metadata contains AUTHENTICATION_FORCELOGIN_ENABLED", readonlyMetadata.contains(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key()));
        assertTrue("Config metadata contains API_LABELS_DICTIONARY", readonlyMetadata.contains(Key.API_LABELS_DICTIONARY.key()));
        assertTrue("Config metadata contains SCHEDULER_NOTIFICATIONS", readonlyMetadata.contains(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key()));
        assertTrue("Config metadata contains PORTAL_ANALYTICS_ENABLED", readonlyMetadata.contains(Key.PORTAL_ANALYTICS_ENABLED.key()));
        assertTrue("Config metadata contains OPEN_API_DOC_TYPE_SWAGGER_ENABLED", readonlyMetadata.contains(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key()));
    }

    @Test
    public void shouldCreatePortalConfig() {
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        portalConfigEntity.getPortal().setUrl("ACME");
        when(mockParameterService.save(PORTAL_URL, "ACME", "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(new Parameter());

        configService.save(portalConfigEntity);

        verify(mockParameterService, times(1)).save(PORTAL_URL, "ACME", "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void shouldGetConsoleConfig() {

        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));

        when(mockParameterService.findAll(any(List.class), eq("DEFAULT"), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        ConsoleConfigEntity consoleConfig = configService.getConsoleConfig();

        assertNotNull(consoleConfig);
        assertEquals("scheduler notifications", Integer.valueOf(11), consoleConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("recaptcha siteKey", "my-site-key", consoleConfig.getReCaptcha().getSiteKey());
        assertEquals("alerting enabled", Boolean.TRUE, consoleConfig.getAlert().getEnabled());
        assertEquals("recaptcha enabled", Boolean.TRUE, consoleConfig.getReCaptcha().getEnabled());
        assertEquals("cors exposed headers", 2, consoleConfig.getCors().getExposedHeaders().size());
    }

    @Test
    public void shouldGetConsoleConfigFromEnvVar() {

        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key(), singletonList("false"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));
        params.put(Key.ANALYTICS_CLIENT_TIMEOUT.key(), singletonList("60000"));
        params.put(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS.key(), singletonList("OnlyOneHeader"));

        when(mockParameterService.findAll(any(List.class), eq("DEFAULT"), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(params);

        when(environment.containsProperty(eq(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);

        ConsoleConfigEntity consoleConfig = configService.getConsoleConfig();

        assertNotNull(consoleConfig);
        assertEquals("scheduler notifications", Integer.valueOf(11), consoleConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("cors exposed headers", 1, consoleConfig.getCors().getExposedHeaders().size());
        List<String> readonlyMetadata = consoleConfig.getMetadata().get(PortalConfigEntity.METADATA_READONLY);
        assertEquals("Config metadata size", 2, readonlyMetadata.size());
        assertTrue("Config metadata contains CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED", readonlyMetadata.contains(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key()));
        assertTrue("Config metadata contains CONSOLE_SCHEDULER_NOTIFICATIONS", readonlyMetadata.contains(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key()));
    }

    @Test
    public void shouldCreateConsoleConfig() {
        ConsoleConfigEntity consoleConfigEntity = new ConsoleConfigEntity();
        consoleConfigEntity.getAlert().setEnabled(true);
        when(mockParameterService.save(ALERT_ENABLED, "true", "DEFAULT", ParameterReferenceType.ORGANIZATION)).thenReturn(new Parameter());

        configService.save(consoleConfigEntity);

        verify(mockParameterService, times(1)).save(ALERT_ENABLED, "true", "DEFAULT", ParameterReferenceType.ORGANIZATION);
    }
}
