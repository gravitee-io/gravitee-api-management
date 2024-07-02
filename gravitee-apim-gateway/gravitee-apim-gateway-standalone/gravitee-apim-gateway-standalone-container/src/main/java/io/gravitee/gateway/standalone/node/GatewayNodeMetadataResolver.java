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
package io.gravitee.gateway.standalone.node;

import static io.gravitee.node.api.Node.META_ENVIRONMENTS;
import static io.gravitee.node.api.Node.META_INSTALLATION;
import static io.gravitee.node.api.Node.META_ORGANIZATIONS;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.node.api.NodeMetadataResolver;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.repository.management.model.Organization;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class GatewayNodeMetadataResolver implements NodeMetadataResolver {

    @Lazy
    @Autowired
    private InstallationRepository installationRepository;

    @Lazy
    @Autowired
    private OrganizationRepository organizationRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private GatewayConfiguration configuration;

    public Map<String, Object> resolve() {
        final HashMap<String, Object> metadata = new HashMap<>();
        metadata.put(META_INSTALLATION, getInstallationId());
        Set<String> organizationIds;
        Set<String> environmentIds;
        if (configuration.useLegacyEnvironmentHrids()) {
            organizationIds = resolveOrganizationsFromHrids();
            environmentIds = resolveEnvironmentFromHrids(organizationIds);
        } else {
            Set<Environment> environments = resolveEnvironments();
            organizationIds = new HashSet<>();
            environmentIds = new HashSet<>();
            environments.forEach(environment -> {
                organizationIds.add(environment.getOrganizationId());
                environmentIds.add(environment.getId());
            });
        }
        metadata.put(META_ORGANIZATIONS, Collections.unmodifiableSet(organizationIds));
        metadata.put(META_ENVIRONMENTS, Collections.unmodifiableSet(environmentIds));

        configuration.tenant().ifPresent(tenant -> metadata.put("tenant", tenant));
        configuration.shardingTags().ifPresent(shardingTags -> metadata.put("tags", shardingTags));
        configuration.zone().ifPresent(zone -> metadata.put("zone", zone));

        return metadata;
    }

    private Set<Environment> resolveEnvironments() {
        final Optional<List<String>> optEnvironmentsList = configuration.environments();
        return optEnvironmentsList
            .stream()
            .flatMap(Collection::stream)
            .filter(id -> {
                if (!validateEnvironmentId(id)) {
                    log.warn("Environment id [%s] should be an valid UUID ; HRIDs are not supported anymore, ignore it.");
                    return false;
                }
                return true;
            })
            .map(id -> {
                try {
                    return environmentRepository.findById(id);
                } catch (TechnicalException e) {
                    log.warn("Unable to load environment from id {}, ignore it.", id, e);
                    return Optional.<Environment>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    private static boolean validateEnvironmentId(final String id) {
        return Objects.equals(id, "DEFAULT") || valideUUID(id);
    }

    private static boolean valideUUID(final String id) {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getInstallationId() {
        try {
            return installationRepository.find().map(Installation::getId).orElse(null);
        } catch (TechnicalException e) {
            log.warn("Unable to load installation id", e);
        }

        return null;
    }

    @Deprecated(forRemoval = true, since = "4.5")
    private Set<String> resolveOrganizationsFromHrids() {
        try {
            final Optional<List<String>> optOrganizationsList = configuration.organizations();

            if (optOrganizationsList.isPresent()) {
                List<String> organizationsList = optOrganizationsList.get();
                if (!organizationsList.isEmpty()) {
                    Set<String> organizationHrids = new HashSet<>(organizationsList);
                    final Set<Organization> organizations = organizationRepository.findByHrids(organizationHrids);
                    final Set<String> organizationIds = organizations.stream().map(Organization::getId).collect(Collectors.toSet());

                    checkOrganizations(organizationHrids, organizations);

                    return organizationIds;
                }
            }
        } catch (Exception e) {
            log.warn("Unable to load organization ids", e);
        }

        return new HashSet<>();
    }

    @Deprecated(forRemoval = true, since = "4.5")
    private Set<String> resolveEnvironmentFromHrids(Set<String> organizationIds) {
        try {
            final Optional<List<String>> optEnvironmentsList = configuration.environments();
            Set<String> environmentHrids = optEnvironmentsList.map(HashSet::new).orElse(new HashSet<>());

            if (!organizationIds.isEmpty() || !environmentHrids.isEmpty()) {
                final Set<Environment> environments = environmentRepository.findByOrganizationsAndHrids(organizationIds, environmentHrids);
                final Set<String> environmentIds = new HashSet<>();

                for (Environment env : environments) {
                    organizationIds.add(env.getOrganizationId());
                    environmentIds.add(env.getId());
                }

                checkEnvironments(environmentHrids, environments);

                return environmentIds;
            }
        } catch (Exception e) {
            log.warn("Unable to load environment ids", e);
        }

        return new HashSet<>();
    }

    private void checkOrganizations(Set<String> organizationsHrids, Set<Organization> organizations) {
        if (organizationsHrids.size() != organizations.size()) {
            final Set<String> hrids = new HashSet<>(organizationsHrids);
            final Set<String> returnedHrids = organizations.stream().flatMap(org -> org.getHrids().stream()).collect(Collectors.toSet());
            hrids.removeAll(returnedHrids);
            log.warn("No organization found for hrids {}", hrids);
        }
    }

    private void checkEnvironments(Set<String> environmentHrids, Set<Environment> environments) {
        final Set<String> returnedHrids = environments
            .stream()
            .flatMap(env -> env.getHrids().stream())
            .filter(environmentHrids::contains)
            .collect(Collectors.toSet());

        if (environmentHrids.size() != returnedHrids.size()) {
            final Set<String> hrids = new HashSet<>(environmentHrids);
            hrids.removeAll(returnedHrids);
            log.warn("No environment found for hrids {}", hrids);
        }
    }
}
