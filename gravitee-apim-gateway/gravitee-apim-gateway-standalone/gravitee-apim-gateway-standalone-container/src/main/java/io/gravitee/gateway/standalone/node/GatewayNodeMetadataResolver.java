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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayNodeMetadataResolver implements NodeMetadataResolver {

    private final Logger logger = LoggerFactory.getLogger(GatewayNodeMetadataResolver.class);

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

        final String installationId = getInstallationId();
        final Set<String> organizationIds = getOrganizationIds();
        final Set<String> environmentIds = getEnvironmentIds(organizationIds);

        configuration.tenant().ifPresent(tenant -> metadata.put("tenant", tenant));

        configuration.shardingTags().ifPresent(shardingTags -> metadata.put("tags", shardingTags));

        configuration.zone().ifPresent(zone -> metadata.put("zone", zone));

        metadata.put(META_INSTALLATION, installationId);
        metadata.put(META_ORGANIZATIONS, organizationIds);
        metadata.put(META_ENVIRONMENTS, environmentIds);

        return metadata;
    }

    private String getInstallationId() {
        try {
            return installationRepository.find().map(Installation::getId).orElse(null);
        } catch (Exception e) {
            logger.warn("Unable to load installation id", e);
        }

        return null;
    }

    private Set<String> getOrganizationIds() {
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
            logger.warn("Unable to load organization ids", e);
        }

        return new HashSet<>();
    }

    private Set<String> getEnvironmentIds(Set<String> organizationIds) {
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
            logger.warn("Unable to load environment ids", e);
        }

        return new HashSet<>();
    }

    private void checkOrganizations(Set<String> organizationsHrids, Set<Organization> organizations) {
        if (organizationsHrids.size() != organizations.size()) {
            final Set<String> hrids = new HashSet<>(organizationsHrids);
            final Set<String> returnedHrids = organizations.stream().flatMap(org -> org.getHrids().stream()).collect(Collectors.toSet());
            hrids.removeAll(returnedHrids);
            logger.warn("No organization found for hrids {}", hrids);
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
            logger.warn("No environment found for hrids {}", hrids);
        }
    }
}
