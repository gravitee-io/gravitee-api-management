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
import type { PortalNavigationItem } from '../types';
import { DB_NAME } from './db';

export function clearPortalsDatabase(): Promise<void> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.deleteDatabase(DB_NAME);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
        request.onblocked = () => resolve();
    });
}

export function buildNavItem(overrides: Partial<PortalNavigationItem> = {}): PortalNavigationItem {
    return {
        id: 'nav-test',
        portalId: 'portal-test',
        title: 'Test Page',
        type: 'PAGE',
        parentId: null,
        order: 0,
        slug: 'test-page-navtest',
        ...overrides,
    } as PortalNavigationItem;
}
