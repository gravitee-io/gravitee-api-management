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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.parameters.Key.ALERT_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.ANALYTICS_CLIENT_TIMEOUT;
import static io.gravitee.rest.api.model.parameters.Key.API_LABELS_DICTIONARY;
import static io.gravitee.rest.api.model.parameters.Key.COMPANY_NAME;
import static io.gravitee.rest.api.model.parameters.Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS;
import static io.gravitee.rest.api.model.parameters.Key.CONSOLE_SCHEDULER_NOTIFICATIONS;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_FROM;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_HOST;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_PASSWORD;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_PORT;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_PROPERTIES_AUTH_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_PROPERTIES_SSL_TRUST;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_PROPERTIES_STARTTLS_ENABLE;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_PROTOCOL;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_SUBJECT;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_USERNAME;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_AUDIT_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_AUDIT_TRAIL_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_DEFAULT_MAX_DURATION;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT;
import static io.gravitee.rest.api.model.parameters.Key.LOGGING_USER_DISPLAYED;
import static io.gravitee.rest.api.model.parameters.Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_ANALYTICS_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS;
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_SCHEDULER_NOTIFICATIONS;
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_URL;
import static io.gravitee.rest.api.model.parameters.Key.TRIAL_INSTANCE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.Email;
import io.gravitee.rest.api.model.settings.Logging;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.model.settings.logging.MessageSampling;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.common.ExecutionContext;
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

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

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

        assertThat(portalSettings).isNotNull();
        assertThat(portalSettings.getAuthentication().getForceLogin().isEnabled()).as("force login").isTrue();
        assertThat(portalSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(portalSettings.getPortal().getAnalytics().isEnabled()).as("analytics").isTrue();
        assertThat(portalSettings.getReCaptcha().getSiteKey()).as("recaptcha siteKey").isEqualTo("my-site-key");
        assertThat(portalSettings.getReCaptcha().getEnabled()).as("recaptcha enabled").isTrue();
        assertThat(portalSettings.getPlan().getSecurity().getKeyless().isEnabled()).as("plan security keyless").isTrue();
        assertThat(portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled())
            .as("open api swagger enabled")
            .isTrue();
        assertThat(portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getDefaultType())
            .as("open api swagger default")
            .isEqualTo("Swagger");
        assertThat(portalSettings.getApi().getLabelsDictionary()).as("api labels").hasSize(2);
        assertThat(portalSettings.getCors().getExposedHeaders()).as("cors exposed headers").hasSize(2);
    }

    @Test
    public void shouldGetPortalSettingsFromEnvVar() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(API_LABELS_DICTIONARY.key(), List.of("label1"));
        params.put(PORTAL_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        params.put(OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key(), singletonList("true"));
        params.put(PORTAL_HTTP_CORS_EXPOSED_HEADERS.key(), singletonList("OnlyOneHeader"));

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

        when(environment.containsProperty(eq(PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.containsProperty(PORTAL_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);
        when(environment.containsProperty(PORTAL_ANALYTICS_ENABLED.key())).thenReturn(true);
        when(environment.containsProperty(OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key())).thenReturn(true);

        PortalSettingsEntity portalSettings = configService.getPortalSettings(GraviteeContext.getExecutionContext());

        assertThat(portalSettings).isNotNull();
        assertThat(portalSettings.getAuthentication().getForceLogin().isEnabled()).as("force login").isTrue();
        assertThat(portalSettings.getApi().getLabelsDictionary()).as("labels dictionary").hasSize(1);
        assertThat(portalSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(portalSettings.getPortal().getAnalytics().isEnabled()).as("analytics").isTrue();
        assertThat(portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled())
            .as("open api swagger enabled")
            .isTrue();
        assertThat(portalSettings.getCors().getExposedHeaders()).as("cors exposed headers").hasSize(1);
        List<String> readonlyMetadata = portalSettings.getMetadata().get(PortalSettingsEntity.METADATA_READONLY);
        assertThat(readonlyMetadata)
            .as("Config metadata size")
            .hasSize(5)
            .as("Config metadata contains AUTHENTICATION_FORCELOGIN_ENABLED")
            .contains(PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key())
            .as("Config metadata contains API_LABELS_DICTIONARY")
            .contains(API_LABELS_DICTIONARY.key())
            .as("Config metadata contains SCHEDULER_NOTIFICATIONS")
            .contains(PORTAL_SCHEDULER_NOTIFICATIONS.key())
            .as("Config metadata contains PORTAL_ANALYTICS_ENABLED")
            .contains(PORTAL_ANALYTICS_ENABLED.key())
            .as("Config metadata contains OPEN_API_DOC_TYPE_SWAGGER_ENABLED")
            .contains(OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key());
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
        params.put(EMAIL_ENABLED.key(), singletonList("true"));

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

        assertThat(consoleSettings).isNotNull();
        assertThat(consoleSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(consoleSettings.getReCaptcha().getSiteKey()).as("recaptcha siteKey").isEqualTo("my-site-key");
        assertThat(consoleSettings.getAlert().getEnabled()).as("alerting enabled").isTrue();
        assertThat(consoleSettings.getReCaptcha().getEnabled()).as("recaptcha enabled").isTrue();
        assertThat(consoleSettings.getCors().getExposedHeaders()).as("cors exposed headers").hasSize(2);
        assertThat(consoleSettings.getAnalyticsPendo().getEnabled()).as("analytics pendo enabled").isFalse();
        assertThat(consoleSettings.getAnalyticsPendo().getApiKey()).as("analytics pendo apiKey").isEmpty();
        assertThat(consoleSettings.getLicenseExpirationNotification().getEnabled())
            .as("license pollInterval notification enabled")
            .isTrue();
        assertThat(consoleSettings.getEmail().getEnabled()).as("email enabled").isTrue();
    }

    @Test
    public void shouldGetConsoleSettingsForTrialInstance() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));
        params.put(EMAIL_ENABLED.key(), singletonList("true"));
        params.put(TRIAL_INSTANCE.key(), singletonList("true"));

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

        assertThat(consoleSettings).isNotNull();
        assertThat(consoleSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(consoleSettings.getReCaptcha().getSiteKey()).as("recaptcha siteKey").isEqualTo("my-site-key");
        assertThat(consoleSettings.getAlert().getEnabled()).as("alerting enabled").isTrue();
        assertThat(consoleSettings.getReCaptcha().getEnabled()).as("recaptcha enabled").isTrue();
        assertThat(consoleSettings.getCors().getExposedHeaders()).as("cors exposed headers").hasSize(2);
        assertThat(consoleSettings.getAnalyticsPendo().getEnabled()).as("analytics pendo enabled").isFalse();
        assertThat(consoleSettings.getAnalyticsPendo().getApiKey()).as("analytics pendo apiKey").isEmpty();
        assertThat(consoleSettings.getLicenseExpirationNotification().getEnabled())
            .as("license pollInterval notification enabled")
            .isTrue();
        assertThat(consoleSettings.getEmail()).as("email should be null").isNull();
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

        assertThat(consoleConfig).isNotNull();
        assertThat(consoleConfig.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(consoleConfig.getReCaptcha().getSiteKey()).as("recaptcha siteKey").isEqualTo("my-site-key");
        assertThat(consoleConfig.getAlert().getEnabled()).as("alerting enabled").isTrue();
        assertThat(consoleConfig.getReCaptcha().getEnabled()).as("recaptcha enabled").isTrue();
        assertThat(consoleConfig.getLicenseExpirationNotification().getEnabled()).as("license pollInterval notification enabled").isTrue();
    }

    @Test
    public void shouldGetConsoleSettingsFromEnvVar() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key(), singletonList("false"));
        params.put(CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(ALERT_ENABLED.key(), singletonList("true"));
        params.put(ANALYTICS_CLIENT_TIMEOUT.key(), singletonList("60000"));
        params.put(CONSOLE_HTTP_CORS_EXPOSED_HEADERS.key(), singletonList("OnlyOneHeader"));

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

        when(environment.containsProperty(eq(CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key()))).thenReturn(true);
        when(environment.containsProperty(CONSOLE_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);

        ConsoleSettingsEntity consoleSettings = configService.getConsoleSettings(GraviteeContext.getExecutionContext());

        assertThat(consoleSettings).isNotNull();
        assertThat(consoleSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(consoleSettings.getCors().getExposedHeaders()).as("cors exposed headers").hasSize(1);
        List<String> readonlyMetadata = consoleSettings.getMetadata().get(PortalSettingsEntity.METADATA_READONLY);
        assertThat(readonlyMetadata)
            .as("Config metadata size")
            .hasSize(2)
            .as("Config metadata contains CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED")
            .contains(CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key())
            .as("Config metadata contains CONSOLE_SCHEDULER_NOTIFICATIONS")
            .contains(CONSOLE_SCHEDULER_NOTIFICATIONS.key());
    }

    @Test
    public void shouldCreateConsoleSettings() {
        ConsoleSettingsEntity consoleSettingsEntity = new ConsoleSettingsEntity();
        consoleSettingsEntity.getAlert().setEnabled(true);
        Logging logging = new Logging();
        logging.setMaxDurationMillis(3000L);
        Logging.Audit audit = new Logging.Audit();
        audit.setEnabled(true);
        Logging.Audit.AuditTrail trail = new Logging.Audit.AuditTrail();
        trail.setEnabled(true);
        audit.setTrail(trail);
        logging.setAudit(audit);
        Logging.User user = new Logging.User();
        user.setDisplayed(true);
        logging.setUser(user);
        final MessageSampling messageSampling = new MessageSampling();
        final MessageSampling.Count count = new MessageSampling.Count();
        count.setDefaultValue(100);
        count.setLimit(10);
        messageSampling.setCount(count);
        final MessageSampling.Probabilistic probabilistic = new MessageSampling.Probabilistic();
        probabilistic.setLimit(0.5);
        probabilistic.setDefaultValue(0.01);
        messageSampling.setProbabilistic(probabilistic);
        final MessageSampling.Temporal temporal = new MessageSampling.Temporal();
        temporal.setLimit("PT1S");
        temporal.setDefaultValue("PT1S");
        messageSampling.setTemporal(temporal);
        logging.setMessageSampling(messageSampling);
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
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT,
                "100",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT,
                "10",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT,
                "PT1S",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT,
                "PT1S",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT,
                "0.01",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(new Parameter());
        when(
            mockParameterService.save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT,
                "0.5",
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
                LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT,
                "100",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT,
                "10",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT,
                "PT1S",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT,
                "PT1S",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT,
                "0.01",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT,
                "0.5",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                LOGGING_AUDIT_TRAIL_ENABLED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
    }

    @Test
    public void shouldCreateConsoleSettingsForTrialInstance() {
        ConsoleSettingsEntity consoleSettingsEntity = new ConsoleSettingsEntity();
        final Email emailSettings = new Email();
        emailSettings.setEnabled(true);
        emailSettings.setHost("test-host");
        emailSettings.setPort(5551);
        emailSettings.setFrom("from");
        emailSettings.setUsername("username");
        emailSettings.setPassword("password");
        emailSettings.setProtocol("protocol");
        emailSettings.setSubject("subject");
        final Email.EmailProperties emailProperties = new Email.EmailProperties();
        emailProperties.setAuth(true);
        emailProperties.setSslTrust("trust");
        emailProperties.setStartTlsEnable(true);
        emailSettings.setProperties(emailProperties);
        consoleSettingsEntity.setEmail(emailSettings);
        consoleSettingsEntity.getTrialInstance().setEnabled(true);

        configService.save(GraviteeContext.getExecutionContext(), consoleSettingsEntity);

        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_ENABLED), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_HOST), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_PORT), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_FROM), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_USERNAME), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_PASSWORD), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_PROTOCOL), any(String.class), any(), any());
        verify(mockParameterService, never()).save(any(ExecutionContext.class), eq(EMAIL_SUBJECT), any(String.class), any(), any());
        verify(mockParameterService, never())
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_AUTH_ENABLED), any(String.class), any(), any());
        verify(mockParameterService, never())
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_SSL_TRUST), any(String.class), any(), any());
        verify(mockParameterService, never())
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_STARTTLS_ENABLE), any(String.class), any(), any());
    }

    @Test
    public void shouldCreateConsoleSettingsWithEmailsForNonTrialInstance() {
        ConsoleSettingsEntity consoleSettingsEntity = new ConsoleSettingsEntity();
        final Email emailSettings = new Email();
        emailSettings.setEnabled(true);
        emailSettings.setHost("test-host");
        emailSettings.setPort(5551);
        emailSettings.setFrom("from");
        emailSettings.setUsername("username");
        emailSettings.setPassword("password");
        emailSettings.setProtocol("protocol");
        emailSettings.setSubject("subject");
        final Email.EmailProperties emailProperties = new Email.EmailProperties();
        emailProperties.setAuth(true);
        emailProperties.setSslTrust("trust");
        emailProperties.setStartTlsEnable(true);
        emailSettings.setProperties(emailProperties);
        consoleSettingsEntity.setEmail(emailSettings);
        consoleSettingsEntity.getTrialInstance().setEnabled(false);

        configService.save(GraviteeContext.getExecutionContext(), consoleSettingsEntity);

        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_ENABLED), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_HOST), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_PORT), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_FROM), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_USERNAME), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_PASSWORD), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_PROTOCOL), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_SUBJECT), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_AUTH_ENABLED), any(String.class), any(), any());
        verify(mockParameterService).save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_SSL_TRUST), any(String.class), any(), any());
        verify(mockParameterService)
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_STARTTLS_ENABLE), any(String.class), any(), any());
    }
}
