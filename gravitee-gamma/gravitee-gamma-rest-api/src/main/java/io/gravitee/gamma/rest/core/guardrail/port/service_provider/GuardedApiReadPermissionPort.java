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
package io.gravitee.gamma.rest.core.guardrail.port.service_provider;

/**
 * Whether the caller may read the definition of an API, which the platform answers from its own permission model.
 *
 * @author GraviteeSource Team
 */
public interface GuardedApiReadPermissionPort {
    /**
     * Whether {@code userId} may read the definition of the API {@code apiId}.
     *
     * @param userId the acting user; never {@code null}, a caller with no identified user reads nothing and has no
     *     question to ask here.
     */
    boolean canRead(String organizationId, String userId, String apiId);
}
