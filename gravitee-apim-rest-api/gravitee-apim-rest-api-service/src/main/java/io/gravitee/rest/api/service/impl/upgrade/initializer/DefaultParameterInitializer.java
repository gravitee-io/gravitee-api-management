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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultParameterInitializer implements Initializer {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultParameterInitializer.class);

    private final ParameterRepository parameterRepository;

    private final ConfigurableEnvironment environment;

    @Autowired
    public DefaultParameterInitializer(@Lazy ParameterRepository parameterRepository, ConfigurableEnvironment environment) {
        this.parameterRepository = parameterRepository;
        this.environment = environment;
    }

    @Override
    public boolean initialize() {
        Parameter managementURLParam = null;
        try {
            String envPortalURL = environment.getProperty("portalURL", Key.MANAGEMENT_URL.defaultValue());
            if (envPortalURL != null) {
                final Optional<Parameter> optionalParameter = parameterRepository.findById(
                    Key.MANAGEMENT_URL.key(),
                    GraviteeContext.getDefaultOrganization(),
                    ParameterReferenceType.ORGANIZATION
                );
                if (!optionalParameter.isPresent()) {
                    managementURLParam = new Parameter();
                    managementURLParam.setKey(Key.MANAGEMENT_URL.key());
                    managementURLParam.setValue(envPortalURL);
                    managementURLParam.setReferenceType(ParameterReferenceType.ORGANIZATION);
                    managementURLParam.setReferenceId(GraviteeContext.getDefaultOrganization());
                    parameterRepository.create(managementURLParam);
                } else if (StringUtils.isEmpty(optionalParameter.get().getValue())) {
                    managementURLParam = optionalParameter.get();
                    managementURLParam.setValue(envPortalURL);
                    parameterRepository.update(managementURLParam);
                }
            }
        } catch (TechnicalException e) {
            logger.error("Error while updating 'management.url' parameter : {}", managementURLParam, e);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return InitializerOrder.DEFAULT_PARAMETER_INITIALIZER;
    }
}
