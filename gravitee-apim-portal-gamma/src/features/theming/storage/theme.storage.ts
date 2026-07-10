/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { PortalThemeDocument } from '../types';
import { runTransaction, THEMES_STORE_NAME } from '../../portals/storage/db';
import { createDefaultThemeDocument, normalizeThemeDocument } from './migrate-legacy-theme';

export async function getTheme(portalId: string): Promise<PortalThemeDocument> {
    const stored = await runTransaction<unknown>(THEMES_STORE_NAME, 'readonly', store =>
        store.get(`theme-${portalId}`),
    );
    return normalizeThemeDocument(stored, portalId);
}

export async function saveTheme(theme: PortalThemeDocument): Promise<void> {
    await runTransaction(THEMES_STORE_NAME, 'readwrite', store => store.put(theme));
}

export async function deleteTheme(portalId: string): Promise<void> {
    await runTransaction(THEMES_STORE_NAME, 'readwrite', store => store.delete(`theme-${portalId}`));
}

export { createDefaultThemeDocument };
