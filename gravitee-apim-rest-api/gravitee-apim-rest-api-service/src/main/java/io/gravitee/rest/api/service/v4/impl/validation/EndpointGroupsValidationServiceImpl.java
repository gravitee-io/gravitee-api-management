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

import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.exceptions.EndpointNameInvalidException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EndpointGroupsValidationService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EndpointGroupsValidationServiceImpl extends TransactionalService implements EndpointGroupsValidationService {

    private final ConnectorService connectorService;

    public EndpointGroupsValidationServiceImpl(final ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @Override
    public List<EndpointGroup> validateAndSanitize(List<EndpointGroup> endpointGroups) {
        if (endpointGroups != null && !endpointGroups.isEmpty()) {
            endpointGroups.forEach(
                endpointGroup -> {
                    validateName(endpointGroup.getName());
                    if (endpointGroup.getEndpoints() != null && !endpointGroups.isEmpty()) {
                        endpointGroup
                            .getEndpoints()
                            .forEach(
                                endpoint -> {
                                    validateName(endpoint.getName());
                                    endpoint.setConfiguration(
                                        // TODO this need to be improved when endpoint connector are implemented in order to check the configuration schema
                                        connectorService.validateConnectorConfiguration(endpoint.getType(), endpoint.getConfiguration())
                                    );
                                }
                            );
                    }
                }
            );
        }
        return endpointGroups;
    }

    private void validateName(final String name) {
        if (name != null && name.contains(":")) {
            throw new EndpointNameInvalidException(name);
        }
    }
}
