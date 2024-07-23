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
package io.gravitee.gateway.services.sync.process.common.deployer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.LicenseFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.CommandRepository;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class DeployerFactory {

    private final ApiKeyService apiKeyService;
    private final SubscriptionService subscriptionService;
    private final PlanService planCache;
    private final Supplier<SubscriptionDispatcher> subscriptionDispatcherSupplier;
    private final CommandRepository commandRepository;
    private final Node node;
    private final ObjectMapper objectMapper;

    private final ApiManager apiManager;

    private final DictionaryManager dictionaryManager;
    private final OrganizationManager organizationManager;

    private final EventManager eventManager;
    private final LicenseManager licenseManager;
    private final LicenseFactory licenseFactory;

    private final AccessPointManager accessPointManager;

    private final SharedPolicyGroupManager sharedPolicyGroupManager;

    private final DistributedSyncService distributedSyncService;

    public SubscriptionDeployer createSubscriptionDeployer() {
        return new SubscriptionDeployer(
            subscriptionService,
            subscriptionDispatcherSupplier.get(),
            commandRepository,
            node,
            objectMapper,
            distributedSyncService
        );
    }

    public ApiKeyDeployer createApiKeyDeployer() {
        return new ApiKeyDeployer(apiKeyService, distributedSyncService);
    }

    public ApiDeployer createApiDeployer() {
        return new ApiDeployer(apiManager, planCache, distributedSyncService);
    }

    public DictionaryDeployer createDictionaryDeployer() {
        return new DictionaryDeployer(dictionaryManager, distributedSyncService);
    }

    public OrganizationDeployer createOrganizationDeployer() {
        return new OrganizationDeployer(organizationManager, distributedSyncService);
    }

    public DebugDeployer createDebugDeployer() {
        return new DebugDeployer(eventManager);
    }

    public LicenseDeployer createLicenseDeployer() {
        return new LicenseDeployer(licenseManager, licenseFactory, distributedSyncService);
    }

    public AccessPointDeployer createAccessPointDeployer() {
        return new AccessPointDeployer(accessPointManager, distributedSyncService);
    }

    public SharedPolicyGroupDeployer createSharedPolicyGroupDeployer() {
        return new SharedPolicyGroupDeployer(sharedPolicyGroupManager, distributedSyncService);
    }
}
