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
import { Checkbox } from '@gravitee/graphene-core';
import { CircleCheckIcon, FolderOpenIcon, TriangleAlertIcon, XIcon } from '@gravitee/graphene-core/icons';

import {
    formatClassicPageLabel,
    isClassicPageSelectable,
    type ClassicPageNode,
} from '../mocks/classic-documentation.fixture';
import styles from './ClassicPageTreePreview.module.scss';

interface ClassicPageTreePreviewProps {
    readonly nodes: readonly ClassicPageNode[];
    readonly selectedIds: ReadonlySet<string>;
    readonly includeWarningPages: boolean;
    readonly onTogglePage: (pageId: string, selected: boolean) => void;
}

function StatusIcon({ status }: { readonly status: ClassicPageNode['status'] }) {
    switch (status) {
        case 'ready':
            return <CircleCheckIcon className={styles.statusReady} aria-hidden="true" />;
        case 'warning':
            return <TriangleAlertIcon className={styles.statusWarning} aria-hidden="true" />;
        case 'skipped':
            return <XIcon className={styles.statusSkipped} aria-hidden="true" />;
    }
}

interface TreeNodeRowProps {
    readonly node: ClassicPageNode;
    readonly depth: number;
    readonly selectedIds: ReadonlySet<string>;
    readonly includeWarningPages: boolean;
    readonly onTogglePage: (pageId: string, selected: boolean) => void;
}

function TreeNodeRow({ node, depth, selectedIds, includeWarningPages, onTogglePage }: TreeNodeRowProps) {
    const selectable = isClassicPageSelectable(node, includeWarningPages);
    const isFolder = node.type === 'FOLDER';

    return (
        <>
            <div className={styles.row} style={{ paddingLeft: `${12 + depth * 16}px` }}>
                {isFolder ? (
                    <span className={styles.folderIcon} aria-hidden="true">
                        <FolderOpenIcon className="size-4" />
                    </span>
                ) : (
                    <Checkbox
                        checked={selectedIds.has(node.id)}
                        disabled={!selectable}
                        aria-label={`Select ${node.title}`}
                        onCheckedChange={checked => onTogglePage(node.id, checked === true)}
                    />
                )}
                <StatusIcon status={node.status} />
                <span className={styles.label}>{formatClassicPageLabel(node)}</span>
            </div>
            {node.children?.map(child => (
                <TreeNodeRow
                    key={child.id}
                    node={child}
                    depth={depth + 1}
                    selectedIds={selectedIds}
                    includeWarningPages={includeWarningPages}
                    onTogglePage={onTogglePage}
                />
            ))}
        </>
    );
}

export function ClassicPageTreePreview({
    nodes,
    selectedIds,
    includeWarningPages,
    onTogglePage,
}: ClassicPageTreePreviewProps) {
    return (
        <div className={styles.tree} data-testid="classic-page-tree">
            {nodes.map(node => (
                <TreeNodeRow
                    key={node.id}
                    node={node}
                    depth={0}
                    selectedIds={selectedIds}
                    includeWarningPages={includeWarningPages}
                    onTogglePage={onTogglePage}
                />
            ))}
        </div>
    );
}
