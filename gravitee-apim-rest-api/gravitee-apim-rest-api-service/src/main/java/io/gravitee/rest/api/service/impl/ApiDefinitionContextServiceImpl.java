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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.DefinitionContextEntity;
import io.gravitee.rest.api.service.ApiDefinitionContextService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ApiDefinitionContextServiceImpl implements ApiDefinitionContextService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiDefinitionContextServiceImpl.class);

    private final ApiRepository apiRepository;

    @Autowired
    public ApiDefinitionContextServiceImpl(@Lazy ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Override
    public void setDefinitionContext(String apiId, DefinitionContextEntity definitionContext) {
        try {
            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
            api.setOrigin(definitionContext.getOrigin());
            api.setMode(definitionContext.getMode());
            api.setSyncFrom(definitionContext.getSyncFrom());
            apiRepository.update(api);
        } catch (TechnicalException e) {
            LOG.error("An error has occurred while trying to set definition context on API " + apiId, e);
            throw new TechnicalManagementException(e);
        }
    }
}
