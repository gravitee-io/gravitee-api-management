/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.imports;

public class ImportJsonNodeWithIds extends ImportJsonNode {

    public static final String CROSS_ID = "crossId";
    public static final String ID = "id";
    public static final String PARENT_ID = "parentId";
    public static final String API = "api";

    public ImportJsonNodeWithIds(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        super(jsonNode);
    }

    public boolean hasCrossId() {
        return hasField(CROSS_ID);
    }

    public String getCrossId() {
        return getField(CROSS_ID);
    }

    public boolean hasId() {
        return hasField(ID);
    }

    public void setId(String id) {
        setField(ID, id);
    }

    public String getId() {
        return getField(ID);
    }

    public boolean hasParentId() {
        return hasField(PARENT_ID);
    }

    public String getParentId() {
        return getField(PARENT_ID);
    }

    public void setParentId(String parentId) {
        setField(PARENT_ID, parentId);
    }

    public void setApi(String id) {
        setField(API, id);
    }
}
