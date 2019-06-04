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
package io.gravitee.rest.api.model.alert;

public enum Metric {
    ERROR("4xx"), SERVER_ERROR("5xx"), NOT_FOUND("404"), INTERNAL_SERVER_ERROR("500");

    private String description;

    Metric(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
