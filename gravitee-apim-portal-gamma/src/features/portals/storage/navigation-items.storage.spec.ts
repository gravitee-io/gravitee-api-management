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
    deleteNavItem,
    getNavItem,
    getNavItems,
    reorderNavItems,
    saveNavItem,
} from './navigation-items.storage';
import { buildNavItem, clearPortalsDatabase } from './navigation-items.storage.test-utils';

describe('navigation-items.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should save and load a navigation item', async () => {
        const item = buildNavItem();

        await saveNavItem(item);

        expect(await getNavItem('nav-test')).toEqual(item);
    });

    it('should return navigation items scoped to a portal', async () => {
        const portalAItem = buildNavItem({ id: 'nav-a', portalId: 'portal-a', order: 0 });
        const portalBItem = buildNavItem({ id: 'nav-b', portalId: 'portal-b', order: 0 });

        await saveNavItem(portalAItem);
        await saveNavItem(portalBItem);

        expect(await getNavItems('portal-a')).toEqual([portalAItem]);
        expect(await getNavItems('portal-b')).toEqual([portalBItem]);
    });

    it('should sort navigation items by order', async () => {
        const second = buildNavItem({ id: 'nav-second', order: 1, title: 'Second' });
        const first = buildNavItem({ id: 'nav-first', order: 0, title: 'First' });

        await saveNavItem(second);
        await saveNavItem(first);

        expect(await getNavItems('portal-test')).toEqual([first, second]);
    });

    it('should delete a navigation item', async () => {
        const item = buildNavItem();

        await saveNavItem(item);
        await deleteNavItem(item.id);

        expect(await getNavItem(item.id)).toBeUndefined();
    });

    it('should reorder navigation items', async () => {
        const first = buildNavItem({ id: 'nav-first', order: 0, title: 'First' });
        const second = buildNavItem({ id: 'nav-second', order: 1, title: 'Second' });
        const third = buildNavItem({ id: 'nav-third', order: 2, title: 'Third' });

        await saveNavItem(first);
        await saveNavItem(second);
        await saveNavItem(third);
        await reorderNavItems('portal-test', ['nav-third', 'nav-first', 'nav-second']);

        expect(await getNavItems('portal-test')).toEqual([
            { ...third, order: 0 },
            { ...first, order: 1 },
            { ...second, order: 2 },
        ]);
    });
});
