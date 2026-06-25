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
import { createDefaultPortalScreenshot } from './dummy-portals';
import {
    deletePortal,
    getAllPortals,
    getPortal,
    savePortal,
    seedPortalsIfEmpty,
} from './portals.storage';
import { clearPortalsDatabase } from './portals.storage.test-utils';

describe('portals.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should save and load a portal', async () => {
        const portal = {
            id: 'portal-1',
            name: 'Test Portal',
            screenshotDataUrl: createDefaultPortalScreenshot('Test Portal'),
            updatedAt: new Date().toISOString(),
        };

        await savePortal(portal);

        expect(await getPortal('portal-1')).toEqual(portal);
        expect(await getAllPortals()).toEqual([portal]);
    });

    it('should delete a portal', async () => {
        const portal = {
            id: 'portal-1',
            name: 'Test Portal',
            screenshotDataUrl: createDefaultPortalScreenshot('Test Portal'),
            updatedAt: new Date().toISOString(),
        };

        await savePortal(portal);
        await deletePortal('portal-1');

        expect(await getPortal('portal-1')).toBeUndefined();
        expect(await getAllPortals()).toEqual([]);
    });

    it('should seed dummy portals when store is empty', async () => {
        const seeded = await seedPortalsIfEmpty();

        expect(seeded).toHaveLength(3);
        expect(await getAllPortals()).toHaveLength(3);
    });

    it('should not re-seed when portals already exist', async () => {
        const firstSeed = await seedPortalsIfEmpty();
        const secondSeed = await seedPortalsIfEmpty();

        expect(secondSeed).toEqual(firstSeed);
        expect(await getAllPortals()).toHaveLength(3);
    });
});
