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
import {
    groupAccessTreeRowsByPortal,
    shouldShowPortalGroupHeaders,
} from './group-access-tree-rows';
import type { AccessTreeRow } from './resolve-nav-item-access';
import { buildNavItem } from '../../portals/storage/navigation-items.storage.test-utils';

function row(portalId: string, rootId: string, id: string, title: string): AccessTreeRow {
    return {
        item: buildNavItem({ id, portalId, title }),
        depth: 0,
        rootId,
        resolved: { access: 'VIEW', inherited: true, grant: null },
    };
}

describe('groupAccessTreeRowsByPortal', () => {
    it('should return a single group when all rows share one embedding', () => {
        const rows = [
            row('portal-a', 'root-a', 'nav-1', 'Getting Started'),
            row('portal-a', 'root-a', 'nav-2', 'Overview'),
        ];

        expect(groupAccessTreeRowsByPortal(rows)).toEqual([
            { rootId: 'root-a', portalId: 'portal-a', rows },
        ]);
        expect(shouldShowPortalGroupHeaders(groupAccessTreeRowsByPortal(rows))).toBe(false);
    });

    it('should keep separate groups for the same portal with different embeddings', () => {
        const a1 = row('portal-a', 'root-1', 'nav-a1', 'Getting Started');
        const a2 = row('portal-a', 'root-2', 'nav-a2', 'Getting Started');
        const a1b = row('portal-a', 'root-1', 'nav-a1b', 'Overview');

        expect(groupAccessTreeRowsByPortal([a1, a2, a1b])).toEqual([
            { rootId: 'root-1', portalId: 'portal-a', rows: [a1, a1b] },
            { rootId: 'root-2', portalId: 'portal-a', rows: [a2] },
        ]);
        expect(shouldShowPortalGroupHeaders(groupAccessTreeRowsByPortal([a1, a2, a1b]))).toBe(true);
    });

    it('should group by embedding across portals and preserve first-seen order', () => {
        const a1 = row('portal-a', 'root-a', 'nav-a1', 'Getting Started');
        const b1 = row('portal-b', 'root-b', 'nav-b1', 'Getting Started');
        const a2 = row('portal-a', 'root-a', 'nav-a2', 'Overview');
        const c1 = row('portal-c', 'root-c', 'nav-c1', 'Getting Started');

        expect(groupAccessTreeRowsByPortal([a1, b1, a2, c1])).toEqual([
            { rootId: 'root-a', portalId: 'portal-a', rows: [a1, a2] },
            { rootId: 'root-b', portalId: 'portal-b', rows: [b1] },
            { rootId: 'root-c', portalId: 'portal-c', rows: [c1] },
        ]);
    });
});
