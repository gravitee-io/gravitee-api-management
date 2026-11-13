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
package io.gravitee.gamma.rest.resources.gravitee_plugin.dto;

import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The wire name of a flow a policy can run in: the kebab-case of its {@link ApiProtocolType} ({@code llm-proxy}). */
public final class PolicyFlow {

    private PolicyFlow() {}

    public static String nameOf(ApiProtocolType flow) {
        return flow.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static Optional<ApiProtocolType> fromName(String name) {
        return Arrays.stream(ApiProtocolType.values())
            .filter(flow -> nameOf(flow).equals(name))
            .findFirst();
    }
}
