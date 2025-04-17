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
import static io.gravitee.rest.api.model.parameters.Key.API_LABELS_DICTIONARY;
import static io.gravitee.rest.api.model.parameters.Key.COMPANY_NAME;
import static io.gravitee.rest.api.model.parameters.Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED;
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
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_SCHEDULER_NOTIFICATIONS;
import static io.gravitee.rest.api.model.parameters.Key.PORTAL_URL;
import static io.gravitee.rest.api.model.parameters.Key.USER_GROUP_REQUIRED_ENABLED;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.AccessPointQueryServiceInMemory;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.Email;
import io.gravitee.rest.api.model.settings.Enabled;
import io.gravitee.rest.api.model.settings.Logging;
import io.gravitee.rest.api.model.settings.PortalConfigEntity;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.model.settings.UserGroup;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

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

    @Mock
    private AccessPointQueryService accessPointQueryService;

    @BeforeEach
    public void setup() {
        GraviteeContext.cleanContext();
        Mockito.reset(mockParameterService);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void shouldGetPortalSettings() {
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
        assertThat(portalSettings.getAuthentication().getForceLogin().isEnabled()).as("force login").isEqualTo(true);
        assertThat(portalSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(portalSettings.getPortal().getAnalytics().isEnabled()).as("analytics").isEqualTo(Boolean.TRUE);
        assertThat(portalSettings.getReCaptcha().getSiteKey()).as("recaptcha siteKey").isEqualTo("my-site-key");
        assertThat(portalSettings.getReCaptcha().getEnabled()).as("recaptcha enabled").isEqualTo(Boolean.TRUE);
        assertThat(portalSettings.getPlan().getSecurity().getKeyless().isEnabled()).as("plan security keyless").isEqualTo(Boolean.TRUE);
        assertThat(portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled())
            .as("open api swagger enabled")
            .isEqualTo(Boolean.TRUE);
        assertThat(portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getDefaultType())
            .as("open api swagger default")
            .isEqualTo("Swagger");
        assertThat(portalSettings.getApi().getLabelsDictionary().size()).as("api labels").isEqualTo(2);
        assertThat(portalSettings.getCors().getExposedHeaders().size()).as("cors exposed headers").isEqualTo(2);
    }

    @Test
    void shouldGetPortalSettingsFromEnvVar() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.API_LABELS_DICTIONARY.key(), List.of("label1"));
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

        lenient().when(environment.containsProperty(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key())).thenReturn(true);
        lenient().when(environment.containsProperty(Key.API_LABELS_DICTIONARY.key())).thenReturn(true);
        lenient().when(environment.containsProperty(Key.PORTAL_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);
        lenient().when(environment.containsProperty(Key.PORTAL_ANALYTICS_ENABLED.key())).thenReturn(true);
        lenient().when(environment.containsProperty(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key())).thenReturn(true);

        PortalSettingsEntity portalSettings = configService.getPortalSettings(GraviteeContext.getExecutionContext());

        assertThat(portalSettings).isNotNull();
        assertThat(portalSettings.getAuthentication().getForceLogin().isEnabled()).as("force login").isTrue();
        assertThat(portalSettings.getApi().getLabelsDictionary()).containsExactly("label1");
        assertThat(portalSettings.getScheduler().getNotificationsInSeconds()).isEqualTo(11);
        assertThat(portalSettings.getPortal().getAnalytics().isEnabled()).as("analytics").isTrue();
        assertThat(portalSettings.getOpenAPIDocViewer().getOpenAPIDocType().getSwagger().isEnabled())
            .as("open api swagger enabled")
            .isTrue();
        assertThat(portalSettings.getCors().getExposedHeaders()).containsExactly("OnlyOneHeader");
        List<String> readonlyMetadata = portalSettings.getMetadata().get(PortalSettingsEntity.METADATA_READONLY);
        assertThat(readonlyMetadata)
            .hasSize(5)
            .containsExactlyInAnyOrder(
                PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED.key(),
                API_LABELS_DICTIONARY.key(),
                PORTAL_SCHEDULER_NOTIFICATIONS.key(),
                PORTAL_ANALYTICS_ENABLED.key(),
                OPEN_API_DOC_TYPE_SWAGGER_ENABLED.key()
            );
    }

    @Test
    void shouldCreatePortalSettings() {
        PortalSettingsEntity portalSettingsEntity = new PortalSettingsEntity();
        portalSettingsEntity.getPortal().setUrl("ACME");

        configService.save(GraviteeContext.getExecutionContext(), portalSettingsEntity);

        verify(mockParameterService, times(1))
            .save(GraviteeContext.getExecutionContext(), PORTAL_URL, "ACME", "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    void shouldGetConsoleSettings() {
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

        assertThat(consoleSettings).isNotNull();
        assertThat(consoleSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(consoleSettings.getReCaptcha().getSiteKey()).as("recaptcha siteKey").isEqualTo("my-site-key");
        assertThat(consoleSettings.getAlert().getEnabled()).as("alerting enabled").isEqualTo(Boolean.TRUE);
        assertThat(consoleSettings.getReCaptcha().getEnabled()).as("recaptcha enabled").isEqualTo(Boolean.TRUE);
        assertThat(consoleSettings.getCors().getExposedHeaders().size()).as("cors exposed headers").isEqualTo(2);
        assertThat(consoleSettings.getAnalyticsPendo().getEnabled()).as("analytics pendo enabled").isEqualTo(Boolean.FALSE);
        assertThat(consoleSettings.getAnalyticsPendo().getApiKey()).as("analytics pendo apiKey").isEqualTo("");
        assertThat(consoleSettings.getLicenseExpirationNotification().getEnabled())
            .as("license expiration notification enabled")
            .isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldGetConsoleConfig() {
        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.ALERT_ENABLED.key(), singletonList("true"));
        params.put(USER_GROUP_REQUIRED_ENABLED.key(), singletonList("true"));

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
        assertThat(consoleConfig.getUserGroup().getRequired().isEnabled()).as("user group required enabled").isTrue();
        assertThat(consoleConfig.getAlert().getEnabled()).as("alerting enabled").isEqualTo(Boolean.TRUE);
        assertThat(consoleConfig.getReCaptcha().getEnabled()).as("recaptcha enabled").isEqualTo(Boolean.TRUE);
        assertThat(consoleConfig.getLicenseExpirationNotification().getEnabled())
            .as("license expiration notification enabled")
            .isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldGetPortalConfig() {
        Map<String, List<String>> params = new HashMap<>();
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
        when(accessPointQueryService.getKafkaGatewayAccessPoints(eq(GraviteeContext.getExecutionContext().getEnvironmentId())))
            .thenReturn(List.of(AccessPoint.builder().host("kafka.local").build()));

        PortalConfigEntity portalConfig = configService.getPortalConfig(GraviteeContext.getExecutionContext());

        assertThat(portalConfig).isNotNull();
        assertThat(portalConfig.getAccessPoints().getKafkaDomains()).containsExactly("kafka.local");
    }

    @Test
    void shouldGetConsoleSettingsFromEnvVar() {
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

        lenient().when(environment.containsProperty(eq(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key()))).thenReturn(true);
        lenient().when(environment.containsProperty(Key.CONSOLE_SCHEDULER_NOTIFICATIONS.key())).thenReturn(true);

        ConsoleSettingsEntity consoleSettings = configService.getConsoleSettings(GraviteeContext.getExecutionContext());

        assertThat(consoleSettings).isNotNull();
        assertThat(consoleSettings.getScheduler().getNotificationsInSeconds()).as("scheduler notifications").isEqualTo(Integer.valueOf(11));
        assertThat(consoleSettings.getCors().getExposedHeaders().size()).as("cors exposed headers").isEqualTo(1);
        List<String> readonlyMetadata = consoleSettings.getMetadata().get(PortalSettingsEntity.METADATA_READONLY);
        assertThat(readonlyMetadata)
            .hasSize(2)
            .contains(CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED.key(), CONSOLE_SCHEDULER_NOTIFICATIONS.key());
    }

    @Test
    void shouldCreateConsoleSettings() {
        ConsoleSettingsEntity consoleSettingsEntity = new ConsoleSettingsEntity();
        consoleSettingsEntity.getAlert().setEnabled(true);
        consoleSettingsEntity.setLogging(
            Logging
                .builder()
                .maxDurationMillis(3000L)
                .audit(Logging.Audit.builder().enabled(true).trail(Logging.Audit.AuditTrail.builder().enabled(true).build()).build())
                .user(Logging.User.builder().displayed(true).build())
                .messageSampling(
                    MessageSampling
                        .builder()
                        .count(MessageSampling.Count.builder().defaultValue(100).limit(10).build())
                        .probabilistic(MessageSampling.Probabilistic.builder().defaultValue(0.01).limit(0.5).build())
                        .temporal(MessageSampling.Temporal.builder().defaultValue("PT1S").limit("PT1S").build())
                        .build()
                )
                .build()
        );
        consoleSettingsEntity.setUserGroup(UserGroup.builder().required(new Enabled(true)).build());

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
        verify(mockParameterService, times(1))
            .save(
                GraviteeContext.getExecutionContext(),
                USER_GROUP_REQUIRED_ENABLED,
                "true",
                "DEFAULT",
                ParameterReferenceType.ORGANIZATION
            );
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldCreateConsoleSettingsForTrialInstance(boolean isTrialInstance) {
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
        consoleSettingsEntity.getTrialInstance().setEnabled(isTrialInstance);

        configService.save(GraviteeContext.getExecutionContext(), consoleSettingsEntity);

        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_ENABLED), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_HOST), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_PORT), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_FROM), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_USERNAME), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_PASSWORD), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_PROTOCOL), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_SUBJECT), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_AUTH_ENABLED), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_SSL_TRUST), any(String.class), any(), any());
        verify(mockParameterService, isTrialInstance ? never() : times(1))
            .save(any(ExecutionContext.class), eq(EMAIL_PROPERTIES_STARTTLS_ENABLE), any(String.class), any(), any());
    }
}
