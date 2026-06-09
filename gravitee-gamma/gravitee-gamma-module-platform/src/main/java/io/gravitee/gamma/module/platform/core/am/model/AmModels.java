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
package io.gravitee.gamma.module.platform.core.am.model;

/** Core domain models for the Access Management integration. */
public final class AmModels {

    private AmModels() {}

    public record Domain(String id, String name, String hrid) {}

    public record Environment(String id, String name) {}

    public record GatewayEntrypoint(String id, String name, String url, boolean defaultEntrypoint) {}

    public record AmConnectionTestResult(boolean ok, Integer status, String message) {
        public static AmConnectionTestResult success() {
            return new AmConnectionTestResult(true, 200, null);
        }

        public static AmConnectionTestResult failure(int status, String message) {
            return new AmConnectionTestResult(false, status, message);
        }
    }
}
