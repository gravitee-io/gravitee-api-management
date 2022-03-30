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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.parameters.Key.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.Logging;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

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

    @Before
    public void setup() {
        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetPortalSettings() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));
        params.put(Key.API_LABELS_DICTIONARY.key(), Arrays.asList("label1", "label2"));

        when(
            mockParameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                any(List.class),
                any(Function.class),
                eq("DEFAULT"),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        PortalSettingsEntity portalSettings = configService.getPortalSettings(GraviteeContext.getExecutionContext());

        assertNotNull(portalSettings);
        assertEquals("force login", true, portalSettings.getAuthentication().getForceLogin().isEnabled());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalSettings.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalSettings.getPortal().getAnalytics().isEnabled());
        assertEquals("recaptcha siteKey", "my-site-key", portalSettings.getReCaptcha().getSiteKey());
        assertEquals("recaptcha enabled", Boolean.TRUE, portalSettings.getReCaptcha().getEnabled());
        assertEquals("plan security keyless", Boolean.TRUE, portalSettings.getPlan().getSecurity().getKeyless().isEnabled());
        assertEquals(
            "open api swagger enabled",
            Boolean.TRUE,
            portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled()
        );
        assertEquals("open api swagger default", "Swagger", portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getDefaultType());
        assertEquals("api labels", 2, portalSettings.getApi().getLabelsDictionary().size());
        assertEquals("cors exposed headers", 2, portalSettings.getCors().getExposedHeaders().size());
    }

    @Test
    public void shouldGetPortalSettingsFromEnvVar() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.API_LABELS_DICTIONARY.key(), Arrays.asList("label1"));
        params.put(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));
        params.put(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS.key(), singletonList("OnlyOneHeader"));

        when(
            mockParameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                any(List.class),
                any(Function.class),
                eq("DEFAULT"),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(params);

        when(environment.containsProperty(eq(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(Key.API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.containsProperty(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);
        when(environment.containsProperty(Key.PORTAL_ANALYTICS_ENABLED.key())).thenReturn(true);
        when(environment.containsProperty(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key())).thenReturn(true);

        PortalSettingsEntity portalSettings = configService.getPortalSettings(GraviteeContext.getExecutionContext());

        assertNotNull(portalSettings);
        assertEquals("force login", true, portalSettings.getAuthentication().getForceLogin().isEnabled());
        assertEquals("labels dictionary", 1, portalSettings.getApi().getLabelsDictionary().size());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalSettings.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalSettings.getPortal().getAnalytics().isEnabled());
        assertEquals(
            "open api swagger enabled",
            Boolean.TRUE,
            portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled()
        );
        assertEquals("cors exposed headers", 1, portalSettings.getCors().getExposedHeaders().size());
        List<String> readonlyMetadata = portalSettings.getMetadata().get(PortalSettingsEntity.METADATA_READONLY);
        assertEquals("Config metadata size", 5, readonlyMetadata.size());
        assertTrue(
            "Config metadata contains AUTHENTICATION_FORCELOGIN_ENABLED",
            readonlyMetadata.contains(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key())
        );
        assertTrue("Config metadata contains API_LABELS_DICTIONARY", readonlyMetadata.contains(Key.API_LABELS_DICTIONARY.key()));
        assertTrue("Config metadata contains SCHEDULER_NOTIFICATIONS", readonlyMetadata.contains(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key()));
        assertTrue("Config metadata contains PORTAL_ANALYTICS_ENABLED", readonlyMetadata.contains(Key.PORTAL_ANALYTICS_ENABLED.key()));
        assertTrue(
            "Config metadata contains OPEN_API_DOC_TYPE_SWAGGER_ENABLED",
            readonlyMetadata.contains(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key())
        );
    }

    @Test
    public void shouldCreatePortalSettings() {
        PortalSettingsEntity portalSettingsEntity = new PortalSettingsEntity();
        portalSettingsEntity.getPortal().setUrl("ACME");
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                PORTAL_URL,
                "ACME",
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(new Parameter());

        configService.save(GraviteeContext.getExecutionContext(), portalSettingsEntity);

        verify(mockParameterService, times(1))
            .save(GraviteeContext.getExecutionContext(), PORTAL_URL, "ACME", "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void shouldGetConsoleSettings() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));

        when(
            mockParameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                any(List.class),
                any(Function.class),
                eq("DEFAULT"),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        ConsoleSettingsEntity consoleSettings = configService.getConsoleSettings(GraviteeContext.getExecutionContext());

        assertNotNull(consoleSettings);
        assertEquals("scheduler notifications", Integer.valueOf(11), consoleSettings.getScheduler().getNotificationsInSeconds());
        assertEquals("recaptcha siteKey", "my-site-key", consoleSettings.getReCaptcha().getSiteKey());
        assertEquals("alerting enabled", Boolean.TRUE, consoleSettings.getAlert().getEnabled());
        assertEquals("recaptcha enabled", Boolean.TRUE, consoleSettings.getReCaptcha().getEnabled());
        assertEquals("cors exposed headers", 2, consoleSettings.getCors().getExposedHeaders().size());
    }

    @Test
    public void shouldGetConsoleConfig() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));

        when(
            mockParameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                any(List.class),
                any(Function.class),
                eq("DEFAULT"),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        ConsoleConfigEntity consoleConfig = configService.getConsoleConfig(GraviteeContext.getExecutionContext());

        assertNotNull(consoleConfig);
        assertEquals("scheduler notifications", Integer.valueOf(11), consoleConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("recaptcha siteKey", "my-site-key", consoleConfig.getReCaptcha().getSiteKey());
        assertEquals("alerting enabled", Boolean.TRUE, consoleConfig.getAlert().getEnabled());
        assertEquals("recaptcha enabled", Boolean.TRUE, consoleConfig.getReCaptcha().getEnabled());
    }

    @Test
    public void shouldGetConsoleSettingsFromEnvVar() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key(), singletonList("false"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));
        params.put(Key.ANALYTICS_CLIENT_TIMEOUT.key(), singletonList("60000"));
        params.put(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS.key(), singletonList("OnlyOneHeader"));

        when(
            mockParameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                any(List.class),
                any(Function.class),
                eq("DEFAULT"),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(params);

        when(environment.containsProperty(eq(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);

        ConsoleSettingsEntity consoleSettings = configService.getConsoleSettings(GraviteeContext.getExecutionContext());

        assertNotNull(consoleSettings);
        assertEquals("scheduler notifications", Integer.valueOf(11), consoleSettings.getScheduler().getNotificationsInSeconds());
        assertEquals("cors exposed headers", 1, consoleSettings.getCors().getExposedHeaders().size());
        List<String> readonlyMetadata = consoleSettings.getMetadata().get(PortalSettingsEntity.METADATA_READONLY);
        assertEquals("Config metadata size", 2, readonlyMetadata.size());
        assertTrue(
            "Config metadata contains CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED",
            readonlyMetadata.contains(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key())
        );
        assertTrue(
            "Config metadata contains CONSOLE_SCHEDULER_NOTIFICATIONS",
            readonlyMetadata.contains(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key())
        );
    }

    @Test
    public void shouldCreateConsoleSettings() {
        ConsoleSettingsEntity consoleSettingsEntity = new ConsoleSettingsEntity();
        consoleSettingsEntity.getAlert().setEnabled(true);
        Logging logging = new Logging();
        logging.setMaxDurationMillis(3000l);
        Logging.Audit audit = new Logging.Audit();
        audit.setEnabled(true);
        Logging.Audit.AuditTrail trail = new Logging.Audit.AuditTrail();
        trail.setEnabled(true);
        audit.setTrail(trail);
        logging.setAudit(audit);
        Logging.User user = new Logging.User();
        user.setDisplayed(true);
        logging.setUser(user);
        consoleSettingsEntity.setLogging(logging);
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                ALERT_ENABLED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_DEFAULT_MAX_DURATION,
                "3000",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_USER_DISPLAYED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_AUDIT_ENABLED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_AUDIT_TRAIL_ENABLED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());

        configService.save(GraviteeContext.getExecutionContext(), consoleSettingsEntity);

        verify(mockParameterService, times(1))
            .save(GraviteeContext.getExecutionContext(), ALERT_ENABLED, "true", "DEFAULT", ParameterReferenceType.ORGANIZATION);
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_DEFAULT_MAX_DURATION,
                "3000",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(GraviteeContext.getExecutionContext(), LOGGING_USER_DISPLAYED, "true", "DEFAULT", ParameterReferenceType.ORGANIZATION);
        verify(mockParameterService, times(1))
            .save(GraviteeContext.getExecutionContext(), LOGGING_AUDIT_ENABLED, "true", "DEFAULT", ParameterReferenceType.ORGANIZATION);
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_AUDIT_TRAIL_ENABLED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
    }
}
