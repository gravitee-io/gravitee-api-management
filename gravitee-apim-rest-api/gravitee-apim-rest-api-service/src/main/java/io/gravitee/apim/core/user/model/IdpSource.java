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
package io.gravitee.apim.core.user.model;

public record IdpSource(String value) {
    public static final IdpSource GRAVITEE = new IdpSource("gravitee");
    public static final IdpSource MEMORY = new IdpSource("memory");

    private static final java.util.Map<String, IdpSource> CACHE = java.util.Map.of("gravitee", GRAVITEE, "memory", MEMORY);

    public static IdpSource of(String value) {
        if (value == null) {
            return null;
        }
        return CACHE.getOrDefault(value, new IdpSource(value));
    }

    public static IdpSource gravitee() {
        return GRAVITEE;
    }
}
