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
package io.gravitee.apim.gateway.integration.tests.fake;

import io.gravitee.policy.api.PolicyConfiguration;
import java.util.List;

/**
 * Configuration of {@link AddHeadersPolicy}, shaped like the one of the transform-headers policy so that the
 * API definitions of the tests can use either.
 */
public record AddHeadersPolicyConfiguration(
    Scope scope,
    List<String> removeHeaders,
    List<Header> addHeaders,
    List<Header> appendHeaders,
    List<String> whitelistHeaders
) implements PolicyConfiguration {
    public enum Scope {
        REQUEST,
        RESPONSE,
    }

    public record Header(String name, String value) {}
}
