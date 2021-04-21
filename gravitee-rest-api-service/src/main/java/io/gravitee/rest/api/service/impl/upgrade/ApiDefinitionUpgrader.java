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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.configuration.identity.*;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

@Component
public class ApiDefinitionUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ApiDefinitionUpgrader.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean upgrade() {
        this.apiRepository.search(null)
            .forEach(
                api -> {
                    try {
                        final Api apiDefinition = this.objectMapper.readValue(api.getDefinition(), Api.class);
                        if (
                            apiDefinition.getDefinitionVersion() == DefinitionVersion.V2 &&
                            apiDefinition.getPaths() != null &&
                            !apiDefinition.getPaths().isEmpty()
                        ) {
                            apiDefinition.setPaths(null);
                        } else if (
                            apiDefinition.getDefinitionVersion() == DefinitionVersion.V1 &&
                            apiDefinition.getFlows() != null &&
                            !apiDefinition.getFlows().isEmpty()
                        ) {
                            apiDefinition.setFlows(null);
                        }
                        api.setDefinition(this.objectMapper.writeValueAsString(apiDefinition));
                        this.apiRepository.update(api);
                    } catch (JsonProcessingException e) {
                        logger.error("Unable to parse the apiDefinition of API {}: {}", api.getId(), e);
                    } catch (TechnicalException e) {
                        logger.error("Problem while updating API {}: {}", api.getId(), e);
                    }
                }
            );
        return true;
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
