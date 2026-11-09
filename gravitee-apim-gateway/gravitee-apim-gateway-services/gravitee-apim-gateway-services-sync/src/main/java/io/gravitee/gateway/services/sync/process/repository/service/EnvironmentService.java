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

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
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

    public void fill(final String environmentId, final ReactableApiProduct reactableApiProduct) {
        if (environmentId != null) {
            Environment apiProductEnv = loadEnvironment(environmentId);
            if (apiProductEnv != null) {
                reactableApiProduct.setEnvironmentId(apiProductEnv.getId());
                reactableApiProduct.setEnvironmentHrid(
                    apiProductEnv.getHrids() != null ? apiProductEnv.getHrids().stream().findFirst().orElse(null) : null
                );

                final io.gravitee.repository.management.model.Organization apiProductOrg = organizations.get(
                    apiProductEnv.getOrganizationId()
                );

                if (apiProductOrg != null) {
                    reactableApiProduct.setOrganizationId(apiProductOrg.getId());
                    reactableApiProduct.setOrganizationHrid(
                        apiProductOrg.getHrids() != null ? apiProductOrg.getHrids().stream().findFirst().orElse(null) : null
                    );
                }
            }
        }
    }

    // The blocking repository calls are intentionally performed outside of ConcurrentHashMap#computeIfAbsent:
    // holding a bin lock across I/O could starve other sync threads. A rare redundant fetch under contention
    // is harmless, and a partial environment (organization not resolved) is never memoized.
    private Environment loadEnvironment(final String environmentId) {
        Environment cached = environments.get(environmentId);
        if (cached != null) {
            return cached;
        }
        try {
            var environmentOpt = environmentRepository.findById(environmentId);
            if (environmentOpt.isEmpty()) {
                // Environment genuinely absent: nothing to cache, deployment continues without enrichment.
                return null;
            }
            Environment environment = environmentOpt.get();
            // Resolve the organization first so a transient failure propagates before the environment is cached.
            loadOrganization(environment);
            environments.put(environmentId, environment);
            return environment;
        } catch (SyncException e) {
            // Transient failure while resolving the organization: do not cache a partial environment,
            // propagate so the sync fails and is retried (self-heals once the repository recovers).
            throw e;
        } catch (Exception e) {
            // Transient failure while fetching the environment: do not cache, propagate for retry.
            throw new SyncException(String.format("An error occurred fetching the environment '%s'.", environmentId), e);
        }
    }

    private void loadOrganization(final Environment environment) {
        final String organizationId = environment.getOrganizationId();
        if (organizationId == null || organizations.containsKey(organizationId)) {
            return;
        }
        try {
            // An absent organization is tolerated (not cached); only a transient failure must fail the
            // sync so the partial environment is never memoized.
            organizationRepository.findById(organizationId).ifPresent(organization -> organizations.put(organizationId, organization));
        } catch (Exception e) {
            throw new SyncException(String.format("An error occurred fetching the organization '%s'.", organizationId), e);
        }
    }
}
