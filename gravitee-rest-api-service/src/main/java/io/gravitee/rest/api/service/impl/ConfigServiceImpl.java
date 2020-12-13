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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.config.*;
import io.gravitee.rest.api.model.config.ConsoleConfigEntity.Enabled;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
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
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        final Authentication auth = getPortalConfig().getAuthentication();
        if ( auth.getForceLogin() != null) {
            result = auth.getForceLogin().isEnabled();
        }
        return result;
    }

    @Override
    public ConsoleConfigEntity getConsoleConfig() {
        return getConfig();
    }

    @Override
    public PortalConfigEntity getPortalConfig() {
        return MAPPER.convertValue(getConfig(), PortalConfigEntity.class);
    }

    private ConsoleConfigEntity getConfig() {
        ConsoleConfigEntity ConsoleConfigEntity = new ConsoleConfigEntity();
        Object[] objects = getObjectArray(ConsoleConfigEntity);

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
        Map<String, List<String>> parameterMap = parameterService.findAll(parameterKeys);

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
                            ConsoleConfigEntity.getMetadata().add(ConsoleConfigEntity.METADATA_READONLY, parameterKey.value().key());
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

        enhanceFromConfigFile(ConsoleConfigEntity);
        return ConsoleConfigEntity;
    }

    private String getFirstValueOrDefault(final List<String> values, final String defaultValue) {
        if (values == null) {
            return defaultValue;
        } else if (values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    private void enhanceFromConfigFile(ConsoleConfigEntity ConsoleConfigEntity) {
        //hack until authent config takes place in the database
        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty("security.providers[" + idx + "].type");
            found = (type != null);
            if (found) {
                String clientId = environment.getProperty("security.providers[" + idx + "].clientId");
                if ("google".equals(type)) {
                    ConsoleConfigEntity.getAuthentication().getGoogle().setClientId(clientId);
                } else if ("github".equals(type)) {
                    ConsoleConfigEntity.getAuthentication().getGithub().setClientId(clientId);
                } else if ("oauth2".equals(type)) {
                    ConsoleConfigEntity.getAuthentication().getOauth2().setClientId(clientId);
                }
            }
            idx++;
        }

        final ReCaptcha reCaptcha = new ReCaptcha();
        reCaptcha.setEnabled(reCaptchaService.isEnabled());
        reCaptcha.setSiteKey(reCaptchaService.getSiteKey());
        ConsoleConfigEntity.setReCaptcha(reCaptcha);

        final Newsletter newsletter = new Newsletter();
        newsletter.setEnabled(newsletterService.isEnabled());
        ConsoleConfigEntity.setNewsletter(newsletter);
    }

    @Override
    public void save(ConsoleConfigEntity ConsoleConfigEntity) {
        Object[] objects = getObjectArray(ConsoleConfigEntity);

        for (Object o : objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    try {
                        if (f.get(o) != null) {
                            if (Enabled.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Boolean.toString(((Enabled) f.get(o)).isEnabled()));
                            } else if (Boolean.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Boolean.toString((Boolean) f.get(o)));
                            } else if (Integer.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Integer.toString((Integer) f.get(o)));
                            } else if (Long.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), Long.toString((Long) f.get(o)));
                            } else if (List.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), (List) f.get(o));
                            } else if (Map.class.isAssignableFrom(f.getType())) {
                                parameterService.save(parameterKey.value(), (Map) f.get(o));
                            } else {
                                parameterService.save(parameterKey.value(), (String) f.get(o));
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

    private Object[] getObjectArray(ConsoleConfigEntity ConsoleConfigEntity) {
        return new Object[]{
                ConsoleConfigEntity,
                ConsoleConfigEntity.getAuthentication(),
                ConsoleConfigEntity.getAuthentication().getGithub(),
                ConsoleConfigEntity.getAuthentication().getGoogle(),
                ConsoleConfigEntity.getAuthentication().getOauth2(),
                ConsoleConfigEntity.getCompany(),
                ConsoleConfigEntity.getDocumentation(),
                ConsoleConfigEntity.getManagement(),
                ConsoleConfigEntity.getPortal(),
                ConsoleConfigEntity.getPortal().getApis(),
                ConsoleConfigEntity.getPortal().getAnalytics(),
                ConsoleConfigEntity.getPortal().getRating(),
                ConsoleConfigEntity.getPortal().getRating().getComment(),
                ConsoleConfigEntity.getPortal().getUploadMedia(),
                ConsoleConfigEntity.getPortal().getUserCreation(),
                ConsoleConfigEntity.getScheduler(),
                ConsoleConfigEntity.getTheme(),
                ConsoleConfigEntity.getPlan(),
                ConsoleConfigEntity.getPlan().getSecurity(),
                ConsoleConfigEntity.getOpenAPIDocViewer(),
                ConsoleConfigEntity.getOpenAPIDocViewer().getOpenAPIDocType(),
                ConsoleConfigEntity.getApiQualityMetrics(),
                ConsoleConfigEntity.getApiReview(),
                ConsoleConfigEntity.getLogging(),
                ConsoleConfigEntity.getLogging().getAudit(),
                ConsoleConfigEntity.getLogging().getUser(),
                ConsoleConfigEntity.getAnalytics(),
                ConsoleConfigEntity.getApplication(),
                ConsoleConfigEntity.getApplication().getRegistration(),
                ConsoleConfigEntity.getApplication().getTypes(),
                ConsoleConfigEntity.getLogging().getAudit().getTrail(),
                ConsoleConfigEntity.getAlert(),
                ConsoleConfigEntity.getMaintenance(),
                ConsoleConfigEntity.getApi(),
                ConsoleConfigEntity.getCors(),
                ConsoleConfigEntity.getEmail(),
                ConsoleConfigEntity.getEmail().getProperties()
        };
    }
}
