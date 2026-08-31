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
import type { Page } from '../../api/types';

export interface PageTreeNode {
    id: string;
    name: string;
    isFolder: boolean;
    children: PageTreeNode[];
}

function isRootParent(parent: string | null | undefined, root: string | undefined): boolean {
    if (root === undefined) {
        return parent === undefined || parent === null || parent === '';
    }
    return parent === root;
}

export function mapToPageTree(pages: readonly Page[], root?: string): PageTreeNode[] {
    return pages
        .filter(page => isRootParent(page.parent, root))
        .sort((left, right) => left.order - right.order)
        .map(page => ({
            id: page.id,
            name: page.name,
            isFolder: page.type === 'FOLDER',
            children: mapToPageTree(pages, page.id),
        }))
        .filter(node => (node.isFolder && node.children.length > 0) || !node.isFolder);
}

export function findFirstPageId(nodes: readonly PageTreeNode[]): string | null {
    for (const node of nodes) {
        if (!node.isFolder) {
            return node.id;
        }
        const nested = findFirstPageId(node.children);
        if (nested) {
            return nested;
        }
    }
    return null;
}

export function containsPage(node: PageTreeNode, pageId: string): boolean {
    if (node.id === pageId) {
        return true;
    }
    return node.children.some(child => containsPage(child, pageId));
}
