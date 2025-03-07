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
package io.gravitee.rest.api.model.log;

import lombok.Getter;

@Getter
public enum LogMetadata {
    UNKNOWN_SERVICE("1"),
    UNKNOWN_SERVICE_MAPPED("?"),
    METADATA_NAME("name"),
    METADATA_DELETED("deleted"),
    METADATA_UNKNOWN("unknown"),
    METADATA_VERSION("version"),
    METADATA_API_TYPE("apiType"),
    METADATA_UNKNOWN_API_NAME("Unknown API (not found)"),
    METADATA_UNKNOWN_APPLICATION_NAME("Unknown application (keyless)"),
    METADATA_UNKNOWN_PLAN_NAME("Unknown plan"),
    METADATA_DELETED_API_NAME("Deleted API"),
    METADATA_DELETED_APPLICATION_NAME("Deleted application"),
    METADATA_DELETED_PLAN_NAME("Deleted plan");

    private final String value;

    LogMetadata(String value) {
        this.value = value;
    }
}
