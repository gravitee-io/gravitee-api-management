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
package io.gravitee.gateway.debug.sync;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class DebugSyncService extends AbstractService implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DebugSyncService.class);

    @Autowired
    private TaskScheduler scheduler;

    @Value("${services.debug.cron:*/5 * * * * *}")
    private String cronTrigger;

    @Value("${services.debug.enabled:true}")
    private boolean enabled;

    @Value("${services.local.enabled:false}")
    private boolean localRegistryEnabled;

    @Autowired
    private DebugSyncManager debugSyncManager;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private GatewayConfiguration configuration;

    private Set<Environment> environments;

    private ScheduledFuture<?> schedule;

    @Override
    protected void doStart() throws Exception {
        if (!localRegistryEnabled) {
            if (enabled) {
                super.doStart();

                logger.info("Sync debug service has been initialized with cron [{}]", cronTrigger);

                this.environments = getTargetedEnvironments();

                schedule = scheduler.schedule(this, new CronTrigger(cronTrigger));
            } else {
                logger.warn("Sync service is disabled");
            }
        } else {
            logger.warn("Sync debug service is disabled because local registry mode is enabled");
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (schedule != null) {
            schedule.cancel(true);
        }
        super.doStop();
    }

    @Override
    public void run() {
        List<String> environmentsIds = environments.stream().map(Environment::getId).collect(Collectors.toList());
        debugSyncManager.refresh(environmentsIds);
    }

    @Override
    protected String name() {
        return "Gateway Debug Sync Service";
    }

    private Set<Environment> getTargetedEnvironments() throws TechnicalException {
        Set<String> organizationsIds = new HashSet<>();

        final Optional<List<String>> optOrganizationsList = configuration.organizations();
        if (optOrganizationsList.isPresent()) {
            List<String> organizationsHrids = optOrganizationsList.get();
            final Set<Organization> organizations = organizationRepository.findByHrids(new HashSet<>(organizationsHrids));
            organizationsIds = organizations.stream().map(Organization::getId).collect(Collectors.toSet());

            checkOrganizations(organizationsHrids, organizations);
        }

        Set<String> environmentsHrids = configuration.environments().map(HashSet::new).orElse(new HashSet<>());
        Set<Environment> environments = environmentRepository.findByOrganizationsAndHrids(organizationsIds, environmentsHrids);

        checkEnvironments(environmentsHrids, environments);

        return environments;
    }

    private void checkOrganizations(List<String> organizationsHrids, Set<Organization> organizations) {
        if (organizationsHrids.size() != organizations.size()) {
            final Set<String> hrids = new HashSet<>(organizationsHrids);
            final Set<String> returnedHrids = organizations.stream().flatMap(org -> org.getHrids().stream()).collect(Collectors.toSet());
            hrids.removeAll(returnedHrids);
            logger.warn("No organization found for hrids {}", hrids);
        }
    }

    private void checkEnvironments(Set<String> environmentsHrids, Set<Environment> environments) {
        final Set<String> returnedHrids = environments
            .stream()
            .flatMap(env -> env.getHrids().stream())
            .filter(environmentsHrids::contains)
            .collect(Collectors.toSet());

        if (environmentsHrids.size() != returnedHrids.size()) {
            final Set<String> hrids = new HashSet<>(environmentsHrids);
            hrids.removeAll(returnedHrids);
            logger.warn("No environment found for hrids {}", hrids);
        }
    }
}
