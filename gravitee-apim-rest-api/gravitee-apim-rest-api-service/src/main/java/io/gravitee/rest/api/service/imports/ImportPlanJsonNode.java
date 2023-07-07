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
package io.gravitee.rest.api.service.imports;

import com.fasterxml.jackson.databind.JsonNode;

public class ImportPlanJsonNode extends ImportJsonNodeWithIds {

    public static final String GENERAL_CONDITIONS = "general_conditions";

    public ImportPlanJsonNode(JsonNode jsonNode) {
        super(jsonNode);
    }

    public boolean hasGeneralConditions() {
        return hasField(GENERAL_CONDITIONS);
    }

    public String getGeneralConditions() {
        return getField(GENERAL_CONDITIONS);
    }

    public void setGeneralConditions(String pageId) {
        setField(GENERAL_CONDITIONS, pageId);
    }
}
