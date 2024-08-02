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

import static io.gravitee.rest.api.service.impl.ParameterServiceImpl.KV_SEPARATOR;
import static io.gravitee.rest.api.service.impl.ParameterServiceImpl.SEPARATOR;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.AbstractCommonSettingsEntity;
import io.gravitee.rest.api.model.settings.CommonAuthentication;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.ConsoleReCaptcha;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.Enabled;
import io.gravitee.rest.api.model.settings.Newsletter;
import io.gravitee.rest.api.model.settings.PortalAuthentication;
import io.gravitee.rest.api.model.settings.PortalConfigEntity;
import io.gravitee.rest.api.model.settings.PortalReCaptcha;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ConfigServiceImpl extends AbstractService implements ConfigService {

    private final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private ReCaptchaService reCaptchaService;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    private static final String SENSITIVE_VALUE = "********";

    @Override
    public boolean portalLoginForced(ExecutionContext executionContext) {
        boolean result = false;
        final PortalAuthentication auth = getPortalSettings(executionContext).getAuthentication();
        if (auth.getForceLogin() != null) {
            result = auth.getForceLogin().isEnabled();
        }
        return result;
    }

    @Override
    public PortalConfigEntity getPortalConfig(ExecutionContext executionContext) {
        return MAPPER.convertValue(getPortalSettings(executionContext), PortalConfigEntity.class);
    }

    @Override
    public PortalSettingsEntity getPortalSettings(ExecutionContext executionContext) {
        PortalSettingsEntity portalConfigEntity = new PortalSettingsEntity();
        Object[] objects = getObjectArray(portalConfigEntity);

        loadConfigByReference(
            executionContext,
            objects,
            portalConfigEntity,
            executionContext.getEnvironmentId(),
            ParameterReferenceType.ENVIRONMENT
        );
        enhanceFromConfigFile(portalConfigEntity);
        enhanceUrlFromService(executionContext, portalConfigEntity);

        return portalConfigEntity;
    }

    @Override
    public ConsoleConfigEntity getConsoleConfig(ExecutionContext executionContext) {
        return MAPPER.convertValue(getConsoleSettings(executionContext), ConsoleConfigEntity.class);
    }

    @Override
    public ConsoleSettingsEntity getConsoleSettings(ExecutionContext executionContext) {
        ConsoleSettingsEntity consoleConfigEntity = new ConsoleSettingsEntity();
        Object[] objects = getObjectArray(consoleConfigEntity);

        loadConfigByReference(
            executionContext,
            objects,
            consoleConfigEntity,
            executionContext.getOrganizationId(),
            ParameterReferenceType.ORGANIZATION
        );
        enhanceFromConfigFile(consoleConfigEntity);

        enhanceUrlFromService(executionContext, consoleConfigEntity);
        hideForTrialInstance(consoleConfigEntity);

        return consoleConfigEntity;
    }

    private void loadConfigByReference(
        ExecutionContext executionContext,
        Object[] objects,
        AbstractCommonSettingsEntity configEntity,
        String referenceId,
        ParameterReferenceType referenceType
    ) {
        // get values from DB
        final List<Key> parameterKeys = new ArrayList<>();
        for (Object o : objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    parameterKeys.add(parameterKey.value());
                }
            }
        }
        Map<String, List<String>> parameterMap = parameterService.findAll(
            executionContext,
            parameterKeys,
            value -> value == null ? null : value.trim(),
            referenceId,
            referenceType
        );

        // set values
        for (Object o : objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    try {
                        List<String> values = parameterMap.get(parameterKey.value().key());
                        if (environment.containsProperty(parameterKey.value().key())) {
                            configEntity.getMetadata().add(PortalSettingsEntity.METADATA_READONLY, parameterKey.value().key());
                        }
                        final String defaultValue = parameterKey.value().defaultValue();
                        if (Enabled.class.isAssignableFrom(f.getType())) {
                            f.set(
                                o,
                                Boolean.parseBoolean(getFirstValueOrDefault(values, defaultValue)) ? new Enabled(true) : new Enabled(false)
                            );
                        } else if (Boolean.class.isAssignableFrom(f.getType())) {
                            f.set(o, Boolean.valueOf(getFirstValueOrDefault(values, defaultValue)));
                        } else if (Integer.class.isAssignableFrom(f.getType())) {
                            f.set(o, Integer.valueOf(getFirstValueOrDefault(values, defaultValue)));
                        } else if (Double.class.isAssignableFrom(f.getType())) {
                            f.set(o, Double.valueOf(getFirstValueOrDefault(values, defaultValue)));
                        } else if (Long.class.isAssignableFrom(f.getType())) {
                            f.set(o, Long.valueOf(getFirstValueOrDefault(values, defaultValue)));
                        } else if (List.class.isAssignableFrom(f.getType())) {
                            if (values == null || values.isEmpty()) {
                                if (StringUtils.isNotEmpty(defaultValue)) {
                                    f.set(o, Arrays.asList(defaultValue.split(SEPARATOR)));
                                } else {
                                    f.set(o, emptyList());
                                }
                            } else {
                                f.set(o, values);
                            }
                        } else if (Map.class.isAssignableFrom(f.getType())) {
                            if (values == null || values.isEmpty()) {
                                if (defaultValue == null) {
                                    f.set(o, emptyMap());
                                } else {
                                    f.set(o, singletonMap(defaultValue.split(KV_SEPARATOR)[0], defaultValue.split(KV_SEPARATOR)[1]));
                                }
                            } else {
                                f.set(
                                    o,
                                    values
                                        .stream()
                                        .collect(
                                            toMap(
                                                v -> v.split(KV_SEPARATOR)[0],
                                                v -> {
                                                    final String[] split = v.split(KV_SEPARATOR);
                                                    if (split.length < 2) {
                                                        return "";
                                                    }
                                                    return split[1];
                                                }
                                            )
                                        )
                                );
                            }
                        } else {
                            // If the parameter contains a sensitive info, we return dummy value
                            if (!parameterKey.sensitive()) {
                                f.set(o, getFirstValueOrDefault(values, defaultValue));
                            } else {
                                f.set(o, SENSITIVE_VALUE);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Unable to set parameter {}. Use the default value", parameterKey.value().key(), e);
                    }
                    f.setAccessible(accessible);
                }
            }
        }
    }

    private String getFirstValueOrDefault(final List<String> values, final String defaultValue) {
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        return values.get(0);
    }

    private void enhanceAuthenticationFromConfigFile(CommonAuthentication authenticationConfig) {
        //hack until authent config takes place in the database
        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty("security.providers[" + idx + "].type");
            found = (type != null);
            if (found) {
                String clientId = environment.getProperty("security.providers[" + idx + "].clientId");
                if ("google".equals(type)) {
                    authenticationConfig.getGoogle().setClientId(clientId);
                } else if ("github".equals(type)) {
                    authenticationConfig.getGithub().setClientId(clientId);
                } else if ("oauth2".equals(type)) {
                    authenticationConfig.getOauth2().setClientId(clientId);
                }
            }
            idx++;
        }
    }

    private void enhanceFromConfigFile(PortalSettingsEntity portalConfigEntity) {
        enhanceAuthenticationFromConfigFile(portalConfigEntity.getAuthentication());
        final PortalReCaptcha reCaptcha = new PortalReCaptcha();
        reCaptcha.setEnabled(reCaptchaService.isEnabled());
        reCaptcha.setSiteKey(reCaptchaService.getSiteKey());
        portalConfigEntity.setReCaptcha(reCaptcha);
    }

    private void enhanceUrlFromService(final ExecutionContext executionContext, final PortalSettingsEntity portalConfigEntity) {
        String portalCustomUrl = installationAccessQueryService.getPortalUrl(executionContext.getEnvironmentId());
        if (portalCustomUrl != null && !portalCustomUrl.equals(portalConfigEntity.getPortal().getUrl())) {
            portalConfigEntity.getPortal().setUrl(portalCustomUrl);
            if (!portalCustomUrl.equals(InstallationAccessQueryService.DEFAULT_PORTAL_URL)) {
                portalConfigEntity.getMetadata().add(PortalSettingsEntity.METADATA_READONLY, Key.PORTAL_URL.key());
            }
        }
    }

    private void enhanceFromConfigFile(ConsoleSettingsEntity consoleSettingsEntity) {
        enhanceAuthenticationFromConfigFile(consoleSettingsEntity.getAuthentication());
        final ConsoleReCaptcha reCaptcha = new ConsoleReCaptcha();
        reCaptcha.setEnabled(reCaptchaService.isEnabled());
        reCaptcha.setSiteKey(reCaptchaService.getSiteKey());
        consoleSettingsEntity.setReCaptcha(reCaptcha);

        final Newsletter newsletter = new Newsletter();
        newsletter.setEnabled(newsletterService.isEnabled());
        consoleSettingsEntity.setNewsletter(newsletter);
    }

    private void enhanceUrlFromService(final ExecutionContext executionContext, final ConsoleSettingsEntity consoleConfigEntity) {
        String consoleCustomUrl = installationAccessQueryService.getConsoleUrl(executionContext.getOrganizationId());
        if (consoleCustomUrl != null && !consoleCustomUrl.equals(consoleConfigEntity.getManagement().getUrl())) {
            consoleConfigEntity.getManagement().setUrl(consoleCustomUrl);
            if (!consoleCustomUrl.equals(InstallationAccessQueryService.DEFAULT_PORTAL_URL)) {
                consoleConfigEntity.getMetadata().add(PortalSettingsEntity.METADATA_READONLY, Key.MANAGEMENT_URL.key());
            }
        }
    }

    private void hideForTrialInstance(final ConsoleSettingsEntity consoleConfigEntity) {
        if (Boolean.TRUE.equals(consoleConfigEntity.getTrialInstance().getEnabled())) {
            consoleConfigEntity.setEmail(null);
        }
    }

    @Override
    public void save(ExecutionContext executionContext, PortalSettingsEntity portalSettingsEntity) {
        Object[] objects = getObjectArray(portalSettingsEntity);
        saveConfigByReference(executionContext, objects, executionContext.getEnvironmentId(), ParameterReferenceType.ENVIRONMENT);
    }

    @Override
    public void save(ExecutionContext executionContext, ConsoleSettingsEntity consoleSettingsEntity) {
        Object[] objects = getObjectArray(consoleSettingsEntity);
        saveConfigByReference(
            executionContext,
            objects,
            executionContext.getOrganizationId(),
            ParameterReferenceType.ORGANIZATION,
            isTrialInstance(consoleSettingsEntity)
        );
    }

    private boolean isTrialInstance(ConsoleSettingsEntity consoleSettings) {
        return (
            consoleSettings.getTrialInstance() != null &&
            consoleSettings.getTrialInstance().getEnabled() != null &&
            consoleSettings.getTrialInstance().getEnabled()
        );
    }

    private void saveConfigByReference(
        ExecutionContext executionContext,
        Object[] objects,
        String referenceId,
        ParameterReferenceType referenceType
    ) {
        saveConfigByReference(executionContext, objects, referenceId, referenceType, false);
    }

    private void saveConfigByReference(
        ExecutionContext executionContext,
        Object[] objects,
        String referenceId,
        ParameterReferenceType referenceType,
        boolean isTrialInstance
    ) {
        for (Object o : objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    // do not save parameters that are hidden for trial instances
                    if (isTrialInstance && parameterKey.value().isHiddenForTrial()) {
                        continue;
                    }
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    try {
                        if (Enabled.class.isAssignableFrom(f.getType())) {
                            final String value = f.get(o) == null ? null : Boolean.toString(((Enabled) f.get(o)).isEnabled());
                            parameterService.save(executionContext, parameterKey.value(), value, referenceId, referenceType);
                        } else if (Boolean.class.isAssignableFrom(f.getType())) {
                            final String value = f.get(o) == null ? null : Boolean.toString((Boolean) f.get(o));
                            parameterService.save(executionContext, parameterKey.value(), value, referenceId, referenceType);
                        } else if (Integer.class.isAssignableFrom(f.getType())) {
                            final String value = f.get(o) == null ? null : Integer.toString((Integer) f.get(o));
                            parameterService.save(executionContext, parameterKey.value(), value, referenceId, referenceType);
                        } else if (Double.class.isAssignableFrom(f.getType())) {
                            final String value = f.get(o) == null ? null : Double.toString((Double) f.get(o));
                            parameterService.save(executionContext, parameterKey.value(), value, referenceId, referenceType);
                        } else if (Long.class.isAssignableFrom(f.getType())) {
                            final String value = f.get(o) == null ? null : Long.toString((Long) f.get(o));
                            parameterService.save(executionContext, parameterKey.value(), value, referenceId, referenceType);
                        } else if (List.class.isAssignableFrom(f.getType())) {
                            parameterService.save(executionContext, parameterKey.value(), (List) f.get(o), referenceId, referenceType);
                        } else if (Map.class.isAssignableFrom(f.getType())) {
                            parameterService.save(executionContext, parameterKey.value(), (Map) f.get(o), referenceId, referenceType);
                        } else {
                            final String value = (String) f.get(o);
                            if (!parameterKey.sensitive() || !SENSITIVE_VALUE.equals(value)) {
                                parameterService.save(
                                    executionContext,
                                    parameterKey.value(),
                                    (String) f.get(o),
                                    referenceId,
                                    referenceType
                                );
                            }
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Unable to set parameter {}. Use the default value", parameterKey.value().key(), e);
                    }
                    f.setAccessible(accessible);
                }
            }
        }
    }

    private Object[] getObjectArray(PortalSettingsEntity portalConfigEntity) {
        return new Object[] {
            portalConfigEntity,
            // Common config
            portalConfigEntity.getEmail(),
            portalConfigEntity.getEmail().getProperties(),
            // Portal Config
            portalConfigEntity.getAnalytics(),
            portalConfigEntity.getApi(),
            portalConfigEntity.getApiQualityMetrics(),
            portalConfigEntity.getApiReview(),
            portalConfigEntity.getApplication(),
            portalConfigEntity.getApplication().getRegistration(),
            portalConfigEntity.getApplication().getTypes(),
            portalConfigEntity.getAuthentication(),
            portalConfigEntity.getAuthentication().getGithub(),
            portalConfigEntity.getAuthentication().getGoogle(),
            portalConfigEntity.getAuthentication().getOauth2(),
            portalConfigEntity.getCompany(),
            portalConfigEntity.getCors(),
            portalConfigEntity.getDocumentation(),
            portalConfigEntity.getOpenAPIDocViewer(),
            portalConfigEntity.getOpenAPIDocViewer().getOpenAPIDocType(),
            portalConfigEntity.getPlan(),
            portalConfigEntity.getPlan().getSecurity(),
            portalConfigEntity.getPortal(),
            portalConfigEntity.getPortal().getApis(),
            portalConfigEntity.getPortal().getAnalytics(),
            portalConfigEntity.getPortal().getRating(),
            portalConfigEntity.getPortal().getRating().getComment(),
            portalConfigEntity.getPortal().getUploadMedia(),
            portalConfigEntity.getPortal().getUserCreation(),
            portalConfigEntity.getPortalNext(),
            portalConfigEntity.getPortalNext().getBanner(),
            portalConfigEntity.getReCaptcha(),
            portalConfigEntity.getScheduler(),
            portalConfigEntity.getDashboards(),
            portalConfigEntity.getDashboards().getApiStatus(),
        };
    }

    private Object[] getObjectArray(ConsoleSettingsEntity consoleConfigEntity) {
        return new Object[] {
            consoleConfigEntity,
            // Common config
            consoleConfigEntity.getEmail(),
            consoleConfigEntity.getEmail().getProperties(),
            // Console Config
            consoleConfigEntity.getAlert(),
            consoleConfigEntity.getAuthentication(),
            consoleConfigEntity.getAuthentication().getGithub(),
            consoleConfigEntity.getAuthentication().getGoogle(),
            consoleConfigEntity.getAuthentication().getOauth2(),
            consoleConfigEntity.getCors(),
            consoleConfigEntity.getReCaptcha(),
            consoleConfigEntity.getScheduler(),
            consoleConfigEntity.getAnalyticsPendo(),
            consoleConfigEntity.getLogging(),
            consoleConfigEntity.getLogging().getAudit(),
            consoleConfigEntity.getLogging().getAudit().getTrail(),
            consoleConfigEntity.getLogging().getUser(),
            consoleConfigEntity.getLogging().getMessageSampling(),
            consoleConfigEntity.getLogging().getMessageSampling().getCount(),
            consoleConfigEntity.getLogging().getMessageSampling().getProbabilistic(),
            consoleConfigEntity.getLogging().getMessageSampling().getTemporal(),
            consoleConfigEntity.getMaintenance(),
            consoleConfigEntity.getManagement(),
            consoleConfigEntity.getNewsletter(),
            consoleConfigEntity.getV4EmulationEngine(),
            consoleConfigEntity.getAlertEngine(),
            consoleConfigEntity.getLicenseExpirationNotification(),
            consoleConfigEntity.getTrialInstance(),
            consoleConfigEntity.getFederation(),
        };
    }
}
