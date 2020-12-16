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

import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.*;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.gravitee.rest.api.model.parameters.AbstractCommonConfigEntity.Enabled;
import static io.gravitee.rest.api.service.impl.ParameterServiceImpl.KV_SEPARATOR;
import static io.gravitee.rest.api.service.impl.ParameterServiceImpl.SEPARATOR;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toMap;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ConfigServiceImpl extends AbstractService implements ConfigService {

    private final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceImpl.class);

    @Autowired
    private ParameterService parameterService;
    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    private NewsletterService newsletterService;
    @Autowired
    private ReCaptchaService reCaptchaService;

    @Override
    public boolean portalLoginForced() {
        boolean result = false;
        final PortalConfigEntity.Authentication auth = getPortalConfig().getAuthentication();
        if ( auth.getForceLogin() != null) {
            result = auth.getForceLogin().isEnabled();
        }
        return result;
    }

    @Override
    public PortalConfigEntity getPortalConfig() {
        return this.getPortalConfig(GraviteeContext.getCurrentEnvironment());
    }

    @Override
    public PortalConfigEntity getPortalConfig(String environmentId) {
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        Object[] objects = getObjectArray(portalConfigEntity);

        loadConfigByReference(objects, portalConfigEntity, environmentId, ParameterReferenceType.ENVIRONMENT);
        enhanceFromConfigFile(portalConfigEntity);

        return portalConfigEntity;
    }

    @Override
    public ConsoleConfigEntity getConsoleConfig() {
        return this.getConsoleConfig(GraviteeContext.getCurrentOrganization());
    }

    @Override
    public ConsoleConfigEntity getConsoleConfig(String organizationId) {
        ConsoleConfigEntity consoleConfigEntity = new ConsoleConfigEntity();
        Object[] objects = getObjectArray(consoleConfigEntity);

        loadConfigByReference(objects, consoleConfigEntity, organizationId, ParameterReferenceType.ORGANIZATION);
        enhanceFromConfigFile(consoleConfigEntity);

        return consoleConfigEntity;
    }

    private void loadConfigByReference(Object[] objects, AbstractCommonConfigEntity configEntity, String referenceId, ParameterReferenceType referenceType) {
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
        Map<String, List<String>> parameterMap = parameterService.findAll(parameterKeys, referenceId, referenceType);

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
                            configEntity.getMetadata().add(PortalConfigEntity.METADATA_READONLY, parameterKey.value().key());
                        }
                        final String defaultValue = parameterKey.value().defaultValue();
                        if (Enabled.class.isAssignableFrom(f.getType())) {
                            f.set(o, Boolean.parseBoolean(getFirstValueOrDefault(values, defaultValue))
                                    ? new Enabled(true)
                                    : new Enabled(false)
                            );
                        } else if (Boolean.class.isAssignableFrom(f.getType())) {
                            f.set(o, Boolean.valueOf(getFirstValueOrDefault(values, defaultValue)));
                        } else if (Integer.class.isAssignableFrom(f.getType())) {
                            f.set(o, Integer.valueOf(getFirstValueOrDefault(values, defaultValue)));
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
                                f.set(o, values.stream().collect(toMap(v -> v.split(KV_SEPARATOR)[0], v -> {
                                    final String[] split = v.split(KV_SEPARATOR);
                                    if (split.length < 2) {
                                        return "";
                                    }
                                    return split[1];
                                })));
                            }
                        } else {
                            f.set(o, getFirstValueOrDefault(values, defaultValue));
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
        if (values == null) {
            return defaultValue;
        } else if (values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    private void enhanceAuthenticationFromConfigFile(AbstractCommonConfigEntity.CommonAuthentication authenticationConfig) {
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


    private void enhanceFromConfigFile(PortalConfigEntity portalConfigEntity) {
        enhanceAuthenticationFromConfigFile(portalConfigEntity.getAuthentication());
        final PortalConfigEntity.ReCaptcha reCaptcha = new PortalConfigEntity.ReCaptcha();
        reCaptcha.setEnabled(reCaptchaService.isEnabled());
        reCaptcha.setSiteKey(reCaptchaService.getSiteKey());
        portalConfigEntity.setReCaptcha(reCaptcha);
    }

    private void enhanceFromConfigFile(ConsoleConfigEntity consoleConfigEntity) {
        enhanceAuthenticationFromConfigFile(consoleConfigEntity.getAuthentication());
        final ConsoleConfigEntity.ConsoleReCaptcha reCaptcha = new ConsoleConfigEntity.ConsoleReCaptcha();
        reCaptcha.setEnabled(reCaptchaService.isEnabled());
        reCaptcha.setSiteKey(reCaptchaService.getSiteKey());
        consoleConfigEntity.setReCaptcha(reCaptcha);

        final ConsoleConfigEntity.Newsletter newsletter = new ConsoleConfigEntity.Newsletter();
        newsletter.setEnabled(newsletterService.isEnabled());
        consoleConfigEntity.setNewsletter(newsletter);
    }

    @Override
    public void save(PortalConfigEntity portalConfigEntity) {
        Object[] objects = getObjectArray(portalConfigEntity);
        saveConfigByReference(objects, GraviteeContext.getCurrentEnvironment(), ParameterReferenceType.ENVIRONMENT);
    }

    @Override
    public void save(ConsoleConfigEntity consoleConfigEntity) {
        Object[] objects = getObjectArray(consoleConfigEntity);
        saveConfigByReference(objects, GraviteeContext.getCurrentOrganization(), ParameterReferenceType.ORGANIZATION);
    }

    private void saveConfigByReference(Object[] objects, String referenceId, ParameterReferenceType referenceType) {
        for (Object o : objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    try {
                        if (f.get(o) != null) {
                            if (Enabled.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Boolean.toString(((Enabled) f.get(o)).isEnabled()), referenceId, referenceType);
                            } else if (Boolean.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Boolean.toString((Boolean) f.get(o)), referenceId, referenceType);
                            } else if (Integer.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Integer.toString((Integer) f.get(o)), referenceId, referenceType);
                            } else if (Long.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Long.toString((Long) f.get(o)), referenceId, referenceType);
                            } else if (List.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), (List) f.get(o), referenceId, referenceType);
                            } else if (Map.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), (Map) f.get(o), referenceId, referenceType);
                            } else {
                                parameterService.save(parameterKey.value(), (String) f.get(o), referenceId, referenceType);
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

    private Object[] getObjectArray(PortalConfigEntity portalConfigEntity) {
        return new Object[]{
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
                portalConfigEntity.getReCaptcha(),
                portalConfigEntity.getScheduler(),
        };
    }

    private Object[] getObjectArray(ConsoleConfigEntity consoleConfigEntity) {
        return new Object[]{
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
                consoleConfigEntity.getLogging(),
                consoleConfigEntity.getLogging().getAudit(),
                consoleConfigEntity.getLogging().getAudit().getTrail(),
                consoleConfigEntity.getLogging().getAudit().getTrail(),
                consoleConfigEntity.getMaintenance(),
                consoleConfigEntity.getManagement(),
                consoleConfigEntity.getNewsletter(),
                consoleConfigEntity.getTheme(),
        };
    }
}
