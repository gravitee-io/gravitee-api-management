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
import type { PortalNavigationItem } from '../../portals/types';
import { findAiWorkspaceAncestor } from './find-ai-workspace-ancestor';
import { findApiAncestor } from './find-api-ancestor';
import { findApiProductAncestor } from './find-api-product-ancestor';

export function canAddAiWorkspaceNavItem(
    navItems: readonly PortalNavigationItem[],
    parentId: string | null,
): boolean {
    return findApiAncestor(navItems, parentId) === null
        && findApiProductAncestor(navItems, parentId) === null
        && findAiWorkspaceAncestor(navItems, parentId) === null;
}
