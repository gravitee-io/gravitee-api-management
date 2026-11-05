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
package io.gravitee.gateway.handlers.api.spring;

import io.gravitee.gateway.handlers.api.manager.ApprovalRuleManager;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalFeature;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalRuleManagerImpl;
import io.gravitee.node.api.license.LicenseManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The approval ports policies get with {@code ctx.getComponent(...)}. They live in the gateway's root context, the parent
 * of every API context; the sync service that fills them runs in a plugin context the policies cannot see.
 */
@Configuration
public class ApprovalConfiguration {

    @Bean
    public ApprovalFeature approvalFeature(LicenseManager licenseManager, Environment environment) {
        // Same switch as the synchronizers that deploy the rules and states (GammaEnabledCondition in gateway-services-sync).
        return new ApprovalFeature(licenseManager, environment.getProperty("gamma.enabled", Boolean.class, false));
    }

    @Bean
    public ApprovalRuleManager approvalRuleManager(ApprovalFeature approvalFeature) {
        return new ApprovalRuleManagerImpl(approvalFeature);
    }
}
