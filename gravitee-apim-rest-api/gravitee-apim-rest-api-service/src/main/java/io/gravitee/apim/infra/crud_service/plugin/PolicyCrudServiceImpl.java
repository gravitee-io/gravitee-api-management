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
package io.gravitee.apim.infra.crud_service.plugin;

import io.gravitee.apim.core.plugin.crud_service.PolicyPluginCrudService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.infra.adapter.PolicyPluginAdapter;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.PolicyNotFoundException;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PolicyCrudServiceImpl implements PolicyPluginCrudService {

    private final PolicyPluginService policyPluginService;

    public PolicyCrudServiceImpl(@Lazy PolicyPluginService policyPluginService) {
        this.policyPluginService = policyPluginService;
    }

    @Override
    public Optional<PolicyPlugin> get(String policyId) {
        log.debug("Find policy by id : {}", policyId);

        try {
            return Optional.of(PolicyPluginAdapter.INSTANCE.map(policyPluginService.findById(policyId)));
        } catch (PluginNotFoundException e) {
            return Optional.empty();
        }
    }
}
