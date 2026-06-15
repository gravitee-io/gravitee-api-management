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
package io.gravitee.gamma.definition.entityid;

/**
 * How a type's slug is produced (ADR-037).
 */
public enum IdentityFamily {
    /** Slug derived from the display name (catalog kinds, proxies). */
    NAME_SEEDED,
    /**
     * Slug derived from a stable attribute so a consumer can rebuild it (e.g. {@code agent-identity} from the
     * {@code clientId} in the JWT). The seeding attribute may be a UUID, so the no-UUID rule does not apply here.
     */
    RECOMPUTABLE,
}
