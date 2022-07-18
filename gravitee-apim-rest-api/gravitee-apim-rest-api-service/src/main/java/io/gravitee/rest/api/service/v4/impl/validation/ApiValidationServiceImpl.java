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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.FlowValidationService;
import io.gravitee.rest.api.service.v4.TagsValidationService;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiValidationServiceImpl extends TransactionalService implements ApiValidationService {

    private final TagsValidationService tagsValidationService;
    private final GroupValidationService groupValidationService;
    private final ListenerValidationService listenerValidationService;
    private final EndpointGroupsValidationService endpointGroupsValidationService;
    private final FlowValidationService flowValidationService;

    public ApiValidationServiceImpl(
        final TagsValidationService tagsValidationService,
        final GroupValidationService groupValidationService,
        final ListenerValidationService listenerValidationService,
        final EndpointGroupsValidationService endpointGroupsValidationService,
        final FlowValidationService flowValidationService
    ) {
        this.tagsValidationService = tagsValidationService;
        this.groupValidationService = groupValidationService;
        this.listenerValidationService = listenerValidationService;
        this.endpointGroupsValidationService = endpointGroupsValidationService;
        this.flowValidationService = flowValidationService;
    }

    @Override
    public void validateAndSanitizeNewApi(
        final ExecutionContext executionContext,
        final NewApiEntity newApiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity
    ) {
        // Validate and clean tags
        newApiEntity.setTags(tagsValidationService.validateAndSanitize(executionContext, null, newApiEntity.getTags()));
        // Validate and clean groups
        newApiEntity.setGroups(groupValidationService.validateAndSanitize(executionContext, newApiEntity.getGroups(), primaryOwnerEntity));
        // Validate and clean listeners
        newApiEntity.setListeners(listenerValidationService.validateAndSanitize(executionContext, null, newApiEntity.getListeners()));
        // Validate and clean endpoints
        newApiEntity.setEndpointGroups(endpointGroupsValidationService.validateAndSanitize(newApiEntity.getEndpointGroups()));
        // Validate and clean flow
        newApiEntity.setFlows(flowValidationService.validateAndSanitize(newApiEntity.getFlows()));
    }
}
