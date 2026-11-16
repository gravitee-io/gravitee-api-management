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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;

/**
 * Told about every product version a runtime is built from: before the runtime is built, and once it has stopped or
 * failed to start. A version may back several runtimes at once while one replaces another.
 */
public interface ApiProductRuntimeListener {
    ApiProductRuntimeListener NOOP = new ApiProductRuntimeListener() {
        @Override
        public void beforeStart(ReactableApiProduct product) {}

        @Override
        public void afterStop(ReactableApiProduct product) {}
    };

    void beforeStart(ReactableApiProduct product);

    void afterStop(ReactableApiProduct product);
}
