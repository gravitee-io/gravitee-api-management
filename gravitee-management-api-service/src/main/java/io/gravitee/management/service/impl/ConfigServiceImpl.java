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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.PortalConfigEntity;
import io.gravitee.management.model.annotations.ParameterKey;
import io.gravitee.management.service.ConfigService;
import io.gravitee.management.service.ParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Override
    public PortalConfigEntity getPortalConfig() {
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        Object[] objects = getObjectArray(portalConfigEntity);

        // get values from DB
        List<String> parameterKeys = new ArrayList<>();
        for (Object o: objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    parameterKeys.add(parameterKey.value().key());
                }
            }
        }
        Map<String, List<String>> parameterMap = parameterService.findAll(parameterKeys);

        // set values
        for (Object o: objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null && parameterMap.containsKey(parameterKey.value().key())) {
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    try {
                        if (PortalConfigEntity.Enabled.class.isAssignableFrom(f.getType())) {
                            f.set(o, Boolean.valueOf(parameterMap.get(parameterKey.value().key()).get(0))
                                    ? PortalConfigEntity.TRUE
                                    : PortalConfigEntity.FALSE
                            );
                        } else if (Boolean.class.isAssignableFrom(f.getType())) {
                            f.set(o, Boolean.valueOf(parameterMap.get(parameterKey.value().key()).get(0)));
                        } else if (Integer.class.isAssignableFrom(f.getType())) {
                            f.set(o, Integer.valueOf(parameterMap.get(parameterKey.value().key()).get(0)));
                        } else if (List.class.isAssignableFrom(f.getType())) {
                            f.set(o, parameterMap.get(parameterKey.value().key()));
                        } else {
                            f.set(o, parameterMap.get(parameterKey.value().key()).get(0));
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
    }

    @Override
    public void save(PortalConfigEntity portalConfigEntity) {
        Object[] objects = getObjectArray(portalConfigEntity);

        for (Object o: objects) {
            for (Field f : o.getClass().getDeclaredFields()) {
                ParameterKey parameterKey = f.getAnnotation(ParameterKey.class);
                if (parameterKey != null) {
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    try {
                        Object value;
                        if (f.get(o) != null && PortalConfigEntity.Enabled.class.isAssignableFrom(f.getType())){
                            value = Boolean.toString(((PortalConfigEntity.Enabled) f.get(o)).isEnabled());
                        } else if (f.get(o) != null && !Collection.class.isAssignableFrom(f.getType())) {
                            value = f.get(o).toString();
                        } else {
                            value = f.get(o);
                        }

                        if (List.class.isAssignableFrom(f.getType())) {
                            parameterService.save(parameterKey.value().key(), (List)value);
                        } else {
                            parameterService.save(parameterKey.value().key(), (String)value);
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
                portalConfigEntity.getPortal().getDashboard(),
                portalConfigEntity.getScheduler(),
                portalConfigEntity.getTheme(),
                portalConfigEntity.getPlan(),
                portalConfigEntity.getPlan().getSecurity()
        };
    }
}
