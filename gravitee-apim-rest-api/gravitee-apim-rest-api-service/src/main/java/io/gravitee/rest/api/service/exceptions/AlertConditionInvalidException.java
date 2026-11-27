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
package io.gravitee.rest.api.service.exceptions;

import java.util.Map;

public class AlertConditionInvalidException extends AbstractManagementException {

    private final String conditionType;
    private final String property;

    public AlertConditionInvalidException(String conditionType, String property) {
        this.conditionType = conditionType;
        this.property = property;
    }

    @Override
    public String getMessage() {
        return "The alert condition of type [" + conditionType + "] is invalid: '" + property + "' is required.";
    }

    @Override
    public int getHttpStatusCode() {
        return 400;
    }

    @Override
    public String getTechnicalCode() {
        return "alert.condition.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("conditionType", conditionType, "property", property);
    }
}
