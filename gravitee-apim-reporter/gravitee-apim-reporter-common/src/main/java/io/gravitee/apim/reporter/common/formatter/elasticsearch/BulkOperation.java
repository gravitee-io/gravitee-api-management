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
package io.gravitee.apim.reporter.common.formatter.elasticsearch;

/**
 * The bulk action of each document. The event metrics and decisions documents always use {@code create}: they are written
 * to data streams in every index mode.
 */
public enum BulkOperation {
    /** Writes the document, replacing one with the same id: refused by data streams. */
    INDEX("index"),
    /** Writes the document unless one with the same id exists, which answers 409: the only action data streams accept. */
    CREATE("create");

    private final String action;

    BulkOperation(String action) {
        this.action = action;
    }

    public String action() {
        return action;
    }
}
