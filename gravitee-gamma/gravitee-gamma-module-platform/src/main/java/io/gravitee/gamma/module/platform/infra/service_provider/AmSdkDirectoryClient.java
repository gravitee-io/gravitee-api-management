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
package io.gravitee.gamma.module.platform.infra.service_provider;

import io.gravitee.gamma.module.platform.core.am.model.AmModels.Domain;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.Environment;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.GatewayEntrypoint;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmDirectoryClient;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkClientFactory.AmApis;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AmSdkDirectoryClient implements AmDirectoryClient {

    private final AmSdkClientFactory clientFactory;

    @Override
    public List<Environment> listEnvironments(String orgId) {
        AmApis apis = clientFactory.forOrg(orgId);
        return AmSdkInvocations.await(
            apis
                .defaults()
                .listEnvironments(orgId)
                .map(envs -> {
                    List<Environment> out = new ArrayList<>();
                    for (var e : envs) out.add(new Environment(e.getId(), e.getName()));
                    return out;
                })
        );
    }

    @Override
    public List<GatewayEntrypoint> listDomainEntrypoints(String orgId, String envId, String domainId) {
        AmApis apis = clientFactory.forOrg(orgId);
        return AmSdkInvocations.await(
            apis
                .domains()
                .getDomainEntrypoints(orgId, envId, domainId)
                .map(entrypoints -> {
                    List<GatewayEntrypoint> out = new ArrayList<>();
                    if (entrypoints != null) {
                        for (var e : entrypoints) {
                            out.add(
                                new GatewayEntrypoint(e.getId(), e.getName(), e.getUrl(), Boolean.TRUE.equals(e.getDefaultEntrypoint()))
                            );
                        }
                    }
                    return out;
                })
        );
    }

    @Override
    public Domain getDomain(String orgId, String envId, String domainId) {
        AmApis apis = clientFactory.forOrg(orgId);
        return AmSdkInvocations.await(
            apis
                .domains()
                .findDomain(orgId, envId, domainId)
                .map(d -> new Domain(d.getId(), d.getName(), d.getHrid()))
        );
    }

    @Override
    public List<Domain> listDomains(String orgId, String envId, String q) {
        // AM matches `q` exactly against the hrid unless it contains wildcards; wrap the term so a
        // partial entry (e.g. "gamma") matches any hrid containing it (e.g. "my-gamma-domain").
        String search = (q == null || q.isBlank()) ? null : "*" + q.strip() + "*";
        AmApis apis = clientFactory.forOrg(orgId);
        return AmSdkInvocations.await(
            apis
                .domains()
                .listDomains(orgId, envId, null, null, search)
                .map(page -> {
                    List<Domain> out = new ArrayList<>();
                    if (page != null && page.getData() != null) {
                        for (var d : page.getData()) out.add(new Domain(d.getId(), d.getName(), d.getHrid()));
                    }
                    return out;
                })
        );
    }
}
