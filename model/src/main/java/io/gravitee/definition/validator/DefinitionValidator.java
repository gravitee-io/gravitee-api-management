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
package io.gravitee.definition.validator;

import io.gravitee.definition.model.*;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.Step;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class DefinitionValidator {

    public static void validate(Api api) {
        if (api.getId() == null) {
            throw new IllegalStateException("[api] Id is required");
        }

        if (api.getName() == null) {
            throw new IllegalStateException("[api] Name is required");
        }

        if (api.getProxy() == null) {
            throw new IllegalStateException("[api] Proxy is required");
        }

        validate(api.getProxy());
        if (api.getResources() != null && !api.getResources().isEmpty()) {
            Set<Resource> deDuplicateList = new HashSet<>(api.getResources());
            if (deDuplicateList.size() < api.getResources().size()) {
                throw new IllegalStateException("[api] Resources name must be unique");
            }
            api.getResources().forEach(DefinitionValidator::validate);
        }

        if (api.getDefinitionVersion() != DefinitionVersion.V1 && api.getPaths() != null && !api.getPaths().isEmpty()) {
            throw new UnsupportedOperationException("[api] Paths are only available for definition 1.x.x");
        }

        if (api.getDefinitionVersion() == DefinitionVersion.V1 && api.getFlows() != null && !api.getFlows().isEmpty()) {
            throw new UnsupportedOperationException("[api] Flows are only available for definition >= 2.x.x");
        }

        if (api.getProperties() != null) {
            validate(api.getProperties());
        }

        if (api.getServices() != null) {
            validate(api.getServices());
        }
    }

    public static void validate(Proxy proxy) {
        if (proxy != null) {
            if (proxy.getCors() != null) {
                validate(proxy.getCors());
            }

            if (proxy.getVirtualHosts() == null || proxy.getVirtualHosts().isEmpty()) {
                throw new IllegalStateException("[proxy] API must define at least a single context_path or one or multiple virtual_hosts");
            }

            // check that group names are unique ==> must use LIST instead of SET
            // final Collection<EndpointGroup> endpointGroups = proxy.getGroups();
            // if (endpointGroups != null && !endpointGroups.isEmpty()) {
            //     Set<EndpointGroup> deDuplicateList = new HashSet<>(endpointGroups);
            //     if (deDuplicateList.size() < endpointGroups.size()) {
            //         throw new IllegalStateException("[api] API must have single endpoint group names");
            //     }
            // }

            // check that endpoint names are unique (in the same group) ==> must use LIST instead of SET
            //public static void validate(EndpointGroup endpointGroup) {
            //    final Collection<Endpoint> endpoints = endpointGroup.getEndpoints();
            //    if (endpoints != null) {
            //        Set<Endpoint> deDuplicateList = new HashSet<>(endpoints);
            //        if (deDuplicateList.size() < endpoints.size()) {
            //            throw new IllegalStateException("[api] API endpoint names must be unique");
            //        }
            //    }
            //}

            if (proxy.getGroups() != null && !proxy.getGroups().isEmpty()) {
                Set<String> endpointNames = proxy.getGroups().stream().map(EndpointGroup::getName).collect(Collectors.toSet());
                for (EndpointGroup group : proxy.getGroups()) {
                    if (group.getEndpoints() != null) {
                        for (Endpoint endpoint : group.getEndpoints()) {
                            if (endpointNames.contains(endpoint.getName())) {
                                throw new IllegalStateException("[proxy] Endpoint names and group names must be unique");
                            }

                            validate(endpoint);

                            endpointNames.add(endpoint.getName());
                        }
                    }
                }
            }
        }
    }

    public static void validate(Endpoint endpoint) {
        if (endpoint != null) {
            if (endpoint.getName() == null) {
                throw new IllegalStateException("[endpoint] Name is required");
            }

            if (endpoint.getTarget() == null) {
                throw new IllegalStateException("[endpoint] Target is required");
            }
        }
    }

    public static void validate(Services services) {
        validate(services.getDiscoveryService());
        validate(services.getHealthCheckService());
        validate(services.getDynamicPropertyService());
    }

    public static void validate(EndpointDiscoveryService service) {
        if (service != null) {
            if (service.getProvider() == null) {
                throw new IllegalStateException("[endpoint-discovery-service] Provider is required");
            }
        }
    }

    public static void validate(DynamicPropertyService service) {
        if (service != null) {
            if (service.getSchedule() == null) {
                throw new IllegalStateException("[dynamic-property-service] Schedule is required");
            }
            if (service.getProvider() == null) {
                throw new IllegalStateException("[dynamic-property-service] Provider is required");
            }

            if (DynamicPropertyProvider.HTTP == service.getProvider()) {
                validate((HttpDynamicPropertyProviderConfiguration) service.getConfiguration());
            }
        }
    }

    public static void validate(HttpDynamicPropertyProviderConfiguration configuration) {
        if (configuration != null) {
            if (configuration.getUrl() == null) {
                throw new IllegalStateException("[dynamic-property-service] [HTTP] URL is required");
            }

            if (configuration.getSpecification() == null) {
                throw new IllegalStateException("[dynamic-property-service] [HTTP] Specification is required");
            }
        }
    }

    public static void validate(HealthCheckService service) {
        if (service != null) {
            if (service.getSchedule() == null) {
                throw new IllegalStateException("[health-check-service] Schedule is required");
            }
            if (service.getSteps() == null) {
                throw new IllegalStateException("[health-check-service] Steps are required");
            }
            service.getSteps().forEach(DefinitionValidator::validate);
        }
    }

    public static void validate(Step step) {
        if (step.getRequest() == null) {
            throw new IllegalStateException("[health-check-service-step] Request is required");
        }
    }

    public static void validate(Resource resource) {
        if (resource != null) {
            if (resource.getName() == null) {
                throw new IllegalStateException("[resource] Name is required");
            }

            if (resource.getType() == null) {
                throw new IllegalStateException("[resource] Type is required");
            }

            if (resource.getConfiguration() == null) {
                throw new IllegalStateException("[resource] Configuration is required");
            }
        }
    }

    public static void validate(Properties properties) {
        properties
            .getProperties()
            .forEach(
                property -> {
                    if (property.getKey() == null) {
                        throw new IllegalStateException("[property] Key is required");
                    }
                    if (property.getValue() == null) {
                        throw new IllegalStateException("[property] Value is required");
                    }
                }
            );
    }

    public static void validate(Cors cors) {
        if (cors.getAccessControlAllowOrigin() != null) {
            cors
                .getAccessControlAllowOrigin()
                .forEach(
                    text -> {
                        if (!"*".equals(text) && (text.contains("(") || text.contains("[") || text.contains("*"))) {
                            try {
                                Pattern.compile(text);
                            } catch (PatternSyntaxException pse) {
                                throw new IllegalArgumentException("Allow origin regex invalid: " + pse.getMessage());
                            }
                        }
                    }
                );
        }
    }
}
