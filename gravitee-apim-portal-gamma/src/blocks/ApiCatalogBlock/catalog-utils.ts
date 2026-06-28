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
import type { PortalNavigationApi, PortalNavigationItem, PortalNavigationPage } from '../../features/portals/types';

export function getPublishedApiNavItems(
    navItems: readonly PortalNavigationItem[],
): PortalNavigationApi[] {
    return navItems.filter((item): item is PortalNavigationApi => item.type === 'API');
}

export function findFirstChildPage(
    navItems: readonly PortalNavigationItem[],
    parentId: string,
): PortalNavigationPage | undefined {
    return navItems
        .filter((item): item is PortalNavigationPage => item.type === 'PAGE' && item.parentId === parentId)
        .sort((a, b) => a.order - b.order)[0];
}
