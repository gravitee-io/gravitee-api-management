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
package io.gravitee.apim.core.plugin.use_case;

import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.plugin.query_service.PolicyPluginQueryService;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GetPolicyPluginsUseCase {

    private final PolicyPluginQueryService policyPluginQueryService;
    private final PluginFilterByLicenseDomainService licenseChecker;

    public GetPolicyPluginsUseCase(PolicyPluginQueryService policyPluginQueryService, PluginFilterByLicenseDomainService licenseChecker) {
        this.policyPluginQueryService = policyPluginQueryService;
        this.licenseChecker = licenseChecker;
    }

    public Output getPoliciesByOrganization(Input input) {
        return new Output(
            this.licenseChecker.setPluginDeployedStatusDependingOnLicense(policyPluginQueryService.findAll(), input.organizationId)
                .stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PolicyPlugin::getName))))
        );
    }

    public record Input(String organizationId) {}

    public record Output(Set<PolicyPlugin> plugins) {}
}
