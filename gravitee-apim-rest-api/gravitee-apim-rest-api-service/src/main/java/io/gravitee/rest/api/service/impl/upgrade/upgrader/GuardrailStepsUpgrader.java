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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Guardians are renamed Guardrails: the step through which the internal flow of an API runs one changes from
 * {@code guardian-policy} with a {@code guardianId} to {@code guardrail-run} with a {@code guardrailId}.
 *
 * <p>Only the stored definitions are rewritten. The deployed ones keep the former step, which the gateway no longer
 * runs: the APIs listed in the log have to be redeployed.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class GuardrailStepsUpgrader implements Upgrader {

    static final String LEGACY_POLICY = "guardian-policy";
    static final String LEGACY_ID_KEY = "guardianId";
    static final String ID_KEY = "guardrailId";

    private final ApiRepository apiRepository;
    private final EnvironmentRepository environmentRepository;
    private final ObjectMapper objectMapper;

    public GuardrailStepsUpgrader(
        @Lazy ApiRepository apiRepository,
        @Lazy EnvironmentRepository environmentRepository,
        ObjectMapper objectMapper
    ) {
        this.apiRepository = apiRepository;
        this.environmentRepository = environmentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        List<String> migrated = new ArrayList<>();
        for (Environment environment : environmentRepository.findAll()) {
            apiRepository
                .search(
                    new ApiCriteria.Builder().environmentId(environment.getId()).definitionVersion(List.of(DefinitionVersion.V4)).build(),
                    null,
                    new ApiFieldFilter.Builder().excludePicture().build()
                )
                .filter(this::migrate)
                .forEach(api -> migrated.add(api.getId()));
        }
        if (!migrated.isEmpty()) {
            log.warn(
                "{} moved the Guardian steps of {} API(s) to the guardrail-run policy; redeploy them to run their Guardrails: {}",
                getClass().getSimpleName(),
                migrated.size(),
                migrated
            );
        }
        return true;
    }

    private boolean migrate(Api api) {
        try {
            JsonNode definition = objectMapper.readTree(api.getDefinition());
            JsonNode flow = definition.path("internalFlow");
            boolean request = migrateSteps(flow.path("request"));
            boolean response = migrateSteps(flow.path("response"));
            if (!request && !response) {
                return false;
            }
            api.setDefinition(objectMapper.writeValueAsString(definition));
            api.setUpdatedAt(new Date());
            apiRepository.update(api);
            return true;
        } catch (Exception e) {
            // one unreadable definition must not keep the platform from starting: the API is left as it is
            log.warn("Unable to move the Guardian steps of the API [{}], skipping it", api.getId(), e);
            return false;
        }
    }

    private static boolean migrateSteps(JsonNode steps) {
        boolean migrated = false;
        for (JsonNode step : steps) {
            if (step instanceof ObjectNode stepNode && LEGACY_POLICY.equals(stepNode.path("policy").asText())) {
                stepNode.put("policy", GuardrailSteps.POLICY);
                if (stepNode.get("configuration") instanceof ObjectNode configuration && configuration.has(LEGACY_ID_KEY)) {
                    configuration.set(ID_KEY, configuration.remove(LEGACY_ID_KEY));
                }
                migrated = true;
            }
        }
        return migrated;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.GUARDRAIL_STEPS_UPGRADER;
    }
}
