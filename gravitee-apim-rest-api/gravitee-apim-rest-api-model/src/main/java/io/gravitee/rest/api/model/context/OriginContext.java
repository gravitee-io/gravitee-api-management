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
package io.gravitee.rest.api.model.context;

/**
 * Context explaining where an API comes from.
 */
public sealed interface OriginContext {
    Origin origin();

    default String name() {
        return origin().name().toLowerCase();
    }

    enum Origin {
        MANAGEMENT,
        KUBERNETES,
        INTEGRATION,
    }

    record Kubernetes(Origin origin, Kubernetes.Mode mode, String syncFrom) implements OriginContext {
        public Kubernetes(Kubernetes.Mode mode) {
            this(mode, Origin.KUBERNETES.name());
        }

        public Kubernetes(Kubernetes.Mode mode, String syncFrom) {
            this(Origin.KUBERNETES, mode, syncFrom);
        }

        public enum Mode {
            /** Mode indicating the api is fully managed by the origin and so, only the origin should be able to manage the api. */
            FULLY_MANAGED,
        }
    }

    record Management(Origin origin) implements OriginContext {
        public Management() {
            this(Origin.MANAGEMENT);
        }
    }

    non-sealed class Integration implements OriginContext {

        private final String integrationId;

        public Integration(String integrationId) {
            this.integrationId = integrationId;
        }

        @Override
        public Origin origin() {
            return Origin.INTEGRATION;
        }

        public String integrationId() {
            return integrationId;
        }
    }
}
