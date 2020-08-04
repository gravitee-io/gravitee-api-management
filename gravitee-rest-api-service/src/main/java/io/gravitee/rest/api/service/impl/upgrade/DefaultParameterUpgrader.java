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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultParameterUpgrader implements Upgrader, Ordered {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultParameterUpgrader.class);

    @Autowired
    private ParameterRepository parameterRepository;
    @Autowired
    private ConfigurableEnvironment environment;

    @Override
    public boolean upgrade() {
        try {
            String envPortalURL = environment.getProperty("portalURL", Key.MANAGEMENT_URL.defaultValue());

            final Optional<Parameter> optionalParameter = parameterRepository.findById(Key.MANAGEMENT_URL.key());
            if (!optionalParameter.isPresent()) {
                Parameter managementURLParam = new Parameter();
                managementURLParam.setKey(Key.MANAGEMENT_URL.key());
                managementURLParam.setValue(envPortalURL);
                managementURLParam.setReferenceType(ParameterReferenceType.ENVIRONMENT);
                managementURLParam.setReferenceId(GraviteeContext.getDefaultEnvironment());
                parameterRepository.create(managementURLParam);
            } else if (StringUtils.isEmpty(optionalParameter.get().getValue())) {
                Parameter managementURLParam = optionalParameter.get();
                managementURLParam.setValue(envPortalURL);
                parameterRepository.update(managementURLParam);
            }
        } catch (TechnicalException e) {
            logger.error("Error while updating 'management.url' parameter : {}", e);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
