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
package io.gravitee.gamma.rest.infra.service_provider.gravitee_plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyConfigurationCheck;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyConfigurationValidator;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Validates through the service the API flows validate their own steps with, so one policy answers the
 * same configuration the same way wherever it is configured, and the schema a step was filled in
 * against in the console is the one it is judged against.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class SchemaPolicyConfigurationValidator implements PolicyConfigurationValidator {

    private final PolicyValidationDomainService policyValidation;
    private final JsonDeserializer jsonDeserializer;

    @Override
    public PolicyConfigurationCheck check(String policyId, String configuration) {
        if (!isJsonObject(configuration)) {
            // held here rather than trusted of the caller: the validator would answer the parser
            // message, which quotes the configuration it could not read, and it may carry a credential
            return new PolicyConfigurationCheck.Violation("it must be a JSON object");
        }
        try {
            return new PolicyConfigurationCheck.Conforming(policyValidation.validateAndSanitizeConfiguration(policyId, configuration));
        } catch (InvalidDataException e) {
            // the message carries the schema paths and keywords the validator reported. A configuration
            // value only appears in it when the schema itself names it — an enum, a pattern — with one
            // exception: a `discriminator` mismatch quotes the discriminating value of the instance.
            return new PolicyConfigurationCheck.Violation(
                Objects.requireNonNullElse(e.getMessage(), "it does not match the schema of the policy")
            );
        } catch (PluginNotFoundException e) {
            // the registry the caller read is a snapshot, and this service resolves the plugin again: a
            // policy uninstalled in between is a refused step, not the 404 this exception would answer
            return new PolicyConfigurationCheck.Violation("the policy is not installed");
        }
    }

    private boolean isJsonObject(String configuration) {
        try {
            JsonNode node = jsonDeserializer.deserialize(configuration, JsonNode.class);
            return node != null && node.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
