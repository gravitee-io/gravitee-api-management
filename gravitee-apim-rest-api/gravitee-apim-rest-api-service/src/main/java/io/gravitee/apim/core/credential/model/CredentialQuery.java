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
package io.gravitee.apim.core.credential.model;

import java.util.Set;
import lombok.Builder;

/**
 * Every filter left empty matches every credential.
 *
 * @param name text the name contains, ignoring case
 * @param strategies a credential matches when its strategy is any of these
 */
@Builder
public record CredentialQuery(String name, Set<CredentialStrategy> strategies, String serviceId) {
    public static CredentialQuery all() {
        return CredentialQuery.builder().build();
    }
}
