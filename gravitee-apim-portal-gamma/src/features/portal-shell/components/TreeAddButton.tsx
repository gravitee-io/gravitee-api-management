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
import type { CSSProperties } from 'react';

import type { PortalNavigationItemType } from '../../portals/types';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import styles from './NavigationTree.module.scss';

interface TreeAddButtonProps {
    readonly parentId: string | null;
    readonly depth: number;
    readonly onAdd: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onRequestApi: (parentId: string | null) => void;
}

export function TreeAddButton({ parentId, depth, onAdd, onRequestApi }: TreeAddButtonProps) {
    const handleAdd = (type: PortalNavigationItemType, itemParentId: string | null) => {
        if (type === 'API') {
            onRequestApi(itemParentId);
            return;
        }
        onAdd(type, itemParentId);
    };

    return (
        <div className={styles.addButtonRow} style={{ '--tree-depth': depth } as CSSProperties}>
            <span className={styles.chevronSpacer} aria-hidden="true" />
            <div className={styles.addButtonSlot}>
                <AddNavItemDropdown
                    allowedTypes={['API', 'FOLDER', 'PAGE', 'LINK']}
                    parentId={parentId}
                    onAdd={handleAdd}
                    className={styles.addButtonTrigger}
                />
            </div>
        </div>
    );
}
