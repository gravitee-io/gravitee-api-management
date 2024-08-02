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
package io.gravitee.gateway.services.sync.process.repository.service;

import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final OrganizationRepository organizationRepository;

    private final Map<String, Environment> environments = new ConcurrentHashMap<>();
    private final Map<String, io.gravitee.repository.management.model.Organization> organizations = new ConcurrentHashMap<>();

    public void fill(final String environmentId, final ReactableApi<?> reactableApi) {
        if (environmentId != null) {
            Environment apiEnv = loadEnvironment(environmentId);
            if (apiEnv != null) {
                reactableApi.setEnvironmentId(apiEnv.getId());
                reactableApi.setEnvironmentHrid(apiEnv.getHrids() != null ? apiEnv.getHrids().stream().findFirst().orElse(null) : null);

                final io.gravitee.repository.management.model.Organization apiOrg = organizations.get(apiEnv.getOrganizationId());

                if (apiOrg != null) {
                    reactableApi.setOrganizationId(apiOrg.getId());
                    reactableApi.setOrganizationHrid(
                        apiOrg.getHrids() != null ? apiOrg.getHrids().stream().findFirst().orElse(null) : null
                    );
                }
            }
        }
    }

    public void fill(final String environmentId, final ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        if (environmentId != null) {
            Environment sharedPolicyGroupEnv = loadEnvironment(environmentId);
            if (sharedPolicyGroupEnv != null) {
                reactableSharedPolicyGroup.setEnvironmentId(sharedPolicyGroupEnv.getId());
                reactableSharedPolicyGroup.setEnvironmentHrid(
                    sharedPolicyGroupEnv.getHrids() != null ? sharedPolicyGroupEnv.getHrids().stream().findFirst().orElse(null) : null
                );

                final io.gravitee.repository.management.model.Organization sharedPolicyGroupOrg = organizations.get(
                    sharedPolicyGroupEnv.getOrganizationId()
                );

                if (sharedPolicyGroupOrg != null) {
                    reactableSharedPolicyGroup.setOrganizationId(sharedPolicyGroupOrg.getId());
                    reactableSharedPolicyGroup.setOrganizationHrid(
                        sharedPolicyGroupOrg.getHrids() != null ? sharedPolicyGroupOrg.getHrids().stream().findFirst().orElse(null) : null
                    );
                }
            }
        }
    }

    private Environment loadEnvironment(final String environmentId) {
        return environments.computeIfAbsent(
            environmentId,
            envId -> {
                try {
                    var environmentOpt = environmentRepository.findById(envId);
                    if (environmentOpt.isPresent()) {
                        Environment environment = environmentOpt.get();
                        loadOrganization(environment);
                        return environment;
                    }
                } catch (Exception e) {
                    log.warn("An error occurred fetching the environment '{}' and its organization.", envId, e);
                }
                return null;
            }
        );
    }

    private void loadOrganization(final Environment environment) {
        organizations.computeIfAbsent(
            environment.getOrganizationId(),
            orgId -> {
                try {
                    return organizationRepository.findById(orgId).orElse(null);
                } catch (Exception e) {
                    return null;
                }
            }
        );
    }
}
