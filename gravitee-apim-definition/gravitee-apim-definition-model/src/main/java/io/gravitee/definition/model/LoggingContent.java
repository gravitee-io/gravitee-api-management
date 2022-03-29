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
package io.gravitee.definition.model;

public enum LoggingContent {
    NONE(false, false),
    HEADERS(true, false),
    PAYLOADS(false, true),
    HEADERS_PAYLOADS(true, true);

    private final boolean headers;
    private final boolean payloads;

    LoggingContent(boolean headers, boolean payloads) {
        this.headers = headers;
        this.payloads = payloads;
    }

    public boolean isHeaders() {
        return headers;
    }

    public boolean isPayloads() {
        return payloads;
    }
}
