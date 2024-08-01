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
package io.gravitee.apim.core.shared_policy_group.exception;

import io.gravitee.rest.api.service.exceptions.AbstractNotFoundException;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class SharedPolicyGroupInvalidPhaseException extends AbstractNotFoundException {

    private final String phase;
    private final String apiType;

    public SharedPolicyGroupInvalidPhaseException(String phase, String apiType) {
        this.phase = phase;
        this.apiType = apiType;
    }

    @Override
    public String getMessage() {
        return String.format("Invalid phase %s for API type %s", this.phase, apiType);
    }

    @Override
    public String getTechnicalCode() {
        return "sharedPolicyGroup.invalidPhase";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("phase", phase, "apiType", apiType);
    }
}
