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
package io.gravitee.repository.management.model;

/**
 * The product a user signed themselves up on.
 *
 * <p>When a registration needs an administrator's approval, the activation email is sent from a later request that
 * knows nothing of the sign-up, so the front door has to be recorded to be honoured. {@code null} means the user
 * predates this being recorded, or was created by an administrator rather than signing up.
 *
 * @author GraviteeSource Team
 */
public enum RegistrationOrigin {
    /** The APIM console. */
    CONSOLE,
    /** The Gamma control plane. */
    GAMMA,
    /** The developer portal. */
    PORTAL,
}
