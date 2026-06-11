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

/** A resource attached to an API definition (cache, OAuth2 provider, auth adapter, …). */
export interface ApiResource {
    name: string;
    /** Resource plugin id (e.g. `cache`, `oauth2`). */
    type: string;
    enabled: boolean;
    configuration?: Record<string, unknown>;
}

/** Resource plugin catalog entry returned by `/v2/organizations/{orgId}/plugins/resources`. */
export interface ResourcePlugin {
    id: string;
    name: string;
    description?: string;
    /** SVG/PNG data-URI provided by the plugin. */
    icon?: string;
    category?: string;
    version?: string;
}
