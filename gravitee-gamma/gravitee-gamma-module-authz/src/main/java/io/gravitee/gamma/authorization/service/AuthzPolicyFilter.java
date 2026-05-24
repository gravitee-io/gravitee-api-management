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
package io.gravitee.gamma.authorization.service;

import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;

public record AuthzPolicyFilter(AuthzPolicyKind kind, String entityId, AuthzPolicyStatus status) {
    public static AuthzPolicyFilter none() {
        return new AuthzPolicyFilter(null, null, null);
    }
}
