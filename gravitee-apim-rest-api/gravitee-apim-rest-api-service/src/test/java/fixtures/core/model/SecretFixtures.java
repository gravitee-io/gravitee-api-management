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
package fixtures.core.model;

import io.gravitee.apim.core.secret.model.Secret;
import java.time.Instant;

public class SecretFixtures {

    private SecretFixtures() {}

    public static final Instant CREATED_AT = Instant.parse("2023-10-22T10:00:00Z");

    public static Secret aSecret() {
        return Secret.builder()
            .id("secret-id")
            .organizationId("org-id")
            .environmentId("env-id")
            .key("anthropic-key")
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();
    }
}
