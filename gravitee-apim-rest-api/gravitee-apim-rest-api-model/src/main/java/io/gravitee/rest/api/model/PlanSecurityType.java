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
package io.gravitee.rest.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
public enum PlanSecurityType {
    /**
     * Plan which is using a key_less (ie. public) security authentication type for incoming HTTP requests.
     */
    KEY_LESS,

    /**
     * Plan which is using an api-key security authentication type for incoming HTTP requests.
     */
    API_KEY,

    /**
     * Plan which is using an OAuth2 security authentication type for incoming HTTP requests.
     */
    OAUTH2,

    /**
     * Plan which is using a JWT security authentication type for incoming HTTP requests.
     */
    JWT,
    /**
     * Plan which is using a mTLS security authentication type for incoming HTTP requests.
     */
    MTLS,
}
