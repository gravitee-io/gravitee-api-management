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
export type AttrValue = string | number | boolean;

/**
 * Where an entity record came from.
 *   - `local`            : hand-created in Authorization (fully editable).
 *   - `apim`             : derived from an APIM-managed API (read-only).
 *   - `gravitee-catalog` : imported from the AIM Context Catalog (read-only).
 */
export type EntitySource = 'local' | 'apim' | 'gravitee-catalog';

export interface EntityInstance {
    uid: { type: string; id: string };
    displayName?: string;
    attrs: Record<string, AttrValue>;
    parents: Array<{ type: string; id: string }>;
    source: EntitySource;
    importedAt?: string;
    _backendId?: string;
    createdAt?: string;
    updatedAt?: string;
}
