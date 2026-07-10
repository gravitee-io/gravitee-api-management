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
import type { PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import type { AddPageOptions } from '../utils/page-type-options';
import { NavigationTree } from './NavigationTree';
import styles from './MobileNavTree.module.scss';

interface MobileNavTreeProps {
    readonly items: PortalNavigationItem[];
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly rootAddParentId?: string | null;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onAddLinkFromPage?: (page: PortalNavigationPage, parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onItemSelect?: () => void;
    readonly instanceOverrides?: Record<string, Record<string, string>>;
}

export function MobileNavTree({
    items,
    allItems,
    selectedNavItemId,
    mode,
    portalId,
    portalPages,
    rootAddParentId = null,
    onSelectNavItem,
    onAddNavItem,
    onAddApiNavItem,
    onAddLinkFromPage,
    onUpdateNavItem,
    onRequestDeleteNavItem,
    onItemSelect,
    instanceOverrides = {},
}: MobileNavTreeProps) {
    const handleSelectNavItem = (id: string) => {
        onSelectNavItem(id);
        onItemSelect?.();
    };

    return (
        <div className={styles.tree}>
            <NavigationTree
                items={items}
                allItems={allItems}
                selectedNavItemId={selectedNavItemId}
                mode={mode}
                portalId={portalId}
                portalPages={portalPages}
                showRootAddButton={mode === 'edit'}
                rootAddParentId={rootAddParentId}
                onSelectNavItem={handleSelectNavItem}
                onAddNavItem={onAddNavItem}
                onAddApiNavItem={onAddApiNavItem}
                onAddLinkFromPage={onAddLinkFromPage}
                onUpdateNavItem={onUpdateNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
                instanceOverrides={instanceOverrides}
            />
        </div>
    );
}
