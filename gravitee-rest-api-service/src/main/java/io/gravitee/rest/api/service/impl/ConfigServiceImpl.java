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

import io.gravitee.rest.api.model.PortalConfigEntity;
import io.gravitee.rest.api.model.PortalConfigEntity.Enabled;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.gravitee.rest.api.service.impl.ParameterServiceImpl.KV_SEPARATOR;
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
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        Object[] objects = getObjectArray(portalConfigEntity);

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
                            portalConfigEntity.getMetadata().add(PortalConfigEntity.METADATA_READONLY, parameterKey.value().key());
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
                                f.set(o, emptyList());
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

        enhanceFromConfigFile(portalConfigEntity);
        return portalConfigEntity;
    }

    private String getFirstValueOrDefault(final List<String> values, final String defaultValue) {
        if (values == null) {
            return defaultValue;
        } else if (values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    private void enhanceFromConfigFile(PortalConfigEntity portalConfigEntity) {
        //hack until authent config takes place in the database
        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty("security.providers[" + idx + "].type");
            found = (type != null);
            if (found) {
                String clientId = environment.getProperty("security.providers[" + idx + "].clientId");
                if ("google".equals(type)) {
                    portalConfigEntity.getAuthentication().getGoogle().setClientId(clientId);
                } else if ("github".equals(type)) {
                    portalConfigEntity.getAuthentication().getGithub().setClientId(clientId);
                } else if ("oauth2".equals(type)) {
                    portalConfigEntity.getAuthentication().getOauth2().setClientId(clientId);
                }
            }
            idx++;
        }

        final PortalConfigEntity.ReCaptcha reCaptcha = new PortalConfigEntity.ReCaptcha();
        reCaptcha.setEnabled(reCaptchaService.isEnabled());
        reCaptcha.setSiteKey(reCaptchaService.getSiteKey());
        portalConfigEntity.setReCaptcha(reCaptcha);

        final PortalConfigEntity.Newsletter newsletter = new PortalConfigEntity.Newsletter();
        newsletter.setEnabled(newsletterService.isEnabled());
        portalConfigEntity.setNewsletter(newsletter);
    }

    @Override
    public void save(PortalConfigEntity portalConfigEntity) {
        Object[] objects = getObjectArray(portalConfigEntity);

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

    private Object[] getObjectArray(PortalConfigEntity portalConfigEntity) {
        return new Object[]{
                portalConfigEntity,
                portalConfigEntity.getAuthentication(),
                portalConfigEntity.getAuthentication().getGithub(),
                portalConfigEntity.getAuthentication().getGoogle(),
                portalConfigEntity.getAuthentication().getOauth2(),
                portalConfigEntity.getCompany(),
                portalConfigEntity.getDocumentation(),
                portalConfigEntity.getManagement(),
                portalConfigEntity.getPortal(),
                portalConfigEntity.getPortal().getApis(),
                portalConfigEntity.getPortal().getAnalytics(),
                portalConfigEntity.getPortal().getRating(),
                portalConfigEntity.getPortal().getRating().getComment(),
                portalConfigEntity.getPortal().getUploadMedia(),
                portalConfigEntity.getPortal().getUserCreation(),
                portalConfigEntity.getScheduler(),
                portalConfigEntity.getTheme(),
                portalConfigEntity.getPlan(),
                portalConfigEntity.getPlan().getSecurity(),
                portalConfigEntity.getOpenAPIDocViewer(),
                portalConfigEntity.getOpenAPIDocViewer().getOpenAPIDocType(),
                portalConfigEntity.getApiQualityMetrics(),
                portalConfigEntity.getApiReview(),
                portalConfigEntity.getLogging(),
                portalConfigEntity.getLogging().getAudit(),
                portalConfigEntity.getLogging().getUser(),
                portalConfigEntity.getAnalytics(),
                portalConfigEntity.getApplication(),
                portalConfigEntity.getApplication().getRegistration(),
                portalConfigEntity.getApplication().getTypes(),
                portalConfigEntity.getLogging().getAudit().getTrail(),
                portalConfigEntity.getAlert(),
                portalConfigEntity.getMaintenance(),
                portalConfigEntity.getApi()
        };
    }
}
