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
package io.gravitee.repository.noop;

import io.gravitee.repository.management.api.*;
import io.gravitee.repository.noop.management.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class NoOpManagementRepositoryConfiguration {

    @Bean
    public EnvironmentRepository environmentRepository() {
        return new NoOpEnvironmentRepository();
    }

    @Bean
    public OrganizationRepository organizationRepository() {
        return new NoOpOrganizationRepository();
    }

    @Bean
    public EventRepository eventRepository() {
        return new NoOpEventRepository();
    }

    @Bean
    public EventLatestRepository eventLatestRepository() {
        return new NoOpEventLatestRepository();
    }

    @Bean
    public InstallationRepository installationRepository() {
        return new NoOpInstallationRepository();
    }

    @Bean
    public PlanRepository planRepository() {
        return new NoOpPlanRepository();
    }

    @Bean
    public SubscriptionRepository subscriptionRepository() {
        return new NoOpSubscriptionRepository();
    }

    @Bean
    public ApiKeyRepository apiKeyRepository() {
        return new NoOpApiKeyRepository();
    }

    @Bean
    public CommandRepository commandRepository() {
        return new NoOpCommandRepository();
    }

    @Bean
    public LicenseRepository licenseRepository() {
        return new NoOpLicenseRepository();
    }

    @Bean
    public AccessPointRepository accessPointRepository() {
        return new NoOpAccessPointRepository();
    }

    @Bean
    public SharedPolicyGroupRepository sharedPolicyGroupRepository() {
        return new NoOpSharedPolicyGroupRepository();
    }
}
