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
 *   - `local`     : hand-created in Authorization (fully editable).
 *   - `scim`      : synced from an external IdP via SCIM (read-only).
 *   - `directory` : synced from the built-in Gravitee User Directory (read-only).
 */
export type EntitySource = 'local' | 'scim' | 'directory';

export interface EntityInstance {
    uid: { type: string; id: string };
    /** Human-readable display name (falls back to attrs.name). */
    displayName?: string;
    attrs: Record<string, AttrValue>;
    parents: Array<{ type: string; id: string }>;
    /** Where this record was materialized from. Drives editability. */
    source: EntitySource;
    /**
     * Identity provider/source label for principals:
     *  - SCIM: the IdP name (e.g. `Okta`, `Azure AD`).
     *  - Directory: the directory name (e.g. `Gravitee User Directory`).
     */
    principalProvider?: string;
    /** ISO timestamp of last import/sync (stored as _importedAt in backend attributes). */
    importedAt?: string;
    /** Backend record id (used for update/delete). Set after fromBackend(). */
    _backendId?: string;
    /** Backend createdAt ISO. */
    createdAt?: string;
    /** Backend updatedAt ISO. */
    updatedAt?: string;
}
