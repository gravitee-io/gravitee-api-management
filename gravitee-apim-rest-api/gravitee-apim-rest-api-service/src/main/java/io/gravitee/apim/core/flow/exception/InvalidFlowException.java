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
package io.gravitee.apim.core.flow.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.ApiType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InvalidFlowException extends ValidationDomainException {

    public InvalidFlowException(String message, Map<String, String> parameters) {
        super(message, parameters);
    }

    public static InvalidFlowException invalidEntrypoint(String flowName, Set<String> invalidEntrypoints) {
        return new InvalidFlowException(
            "The flow [" + flowName + "] contains channel selector with invalid entrypoints",
            withFlowName(flowName, Map.of("invalidEntrypoints", String.join(",", invalidEntrypoints)))
        );
    }

    public static InvalidFlowException invalidSelector(String flowName, ApiType apiType, Set<String> invalidSelectors) {
        return new InvalidFlowException(
            "The flow [" + flowName + "] contains selectors that couldn't apply to " + apiType.getLabel() + " API",
            withFlowName(flowName, Map.of("invalidSelectors", String.join(",", invalidSelectors)))
        );
    }

    public static InvalidFlowException missingPathOperator(String flowName) {
        return new InvalidFlowException(
            "The flow [" + flowName + "] contains an HTTP selector with a missing pathOperator",
            withFlowName(flowName, Map.of())
        );
    }

    public static InvalidFlowException duplicatedSelector(String flowName, Set<String> duplicatedSelectors) {
        return new InvalidFlowException(
            "The flow [" + flowName + "] contains duplicated selectors type",
            withFlowName(flowName, Map.of("duplicatedSelectors", String.join(",", duplicatedSelectors)))
        );
    }

    public static InvalidFlowException invalidWildcardPath(String flowName, String path) {
        return new InvalidFlowException(
            "The flow [" + flowName + "] contains an HTTP selector with an invalid wildcard path",
            withFlowName(flowName, Map.of("path", path))
        );
    }

    private static Map<String, String> withFlowName(String flowName, Map<String, String> parameters) {
        if (flowName == null) {
            return parameters;
        }
        var withName = new HashMap<>(parameters);
        withName.put("flowName", flowName);
        return withName;
    }
}
