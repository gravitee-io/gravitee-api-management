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
import type { BlockNoteDocument } from '../../features/portals/types';

/** Matches catalog grid minimum column width (see CatalogView.module.scss). */
export const TILE_WIDTH_PX = 220;

/** Maximum top-level blocks allowed in a tile template. */
export const MAX_TILE_BLOCKS = 6;

export const DEFAULT_TILE_TEMPLATE: BlockNoteDocument = [
    {
        type: 'graviteeApiMetadata',
        props: { field: 'name' },
        children: [],
    },
    {
        type: 'graviteeApiMetadata',
        props: { field: 'description' },
        children: [],
    },
    {
        type: 'graviteeApiMetadata',
        props: { field: 'labels' },
        children: [],
    },
];

export function serializeTileTemplate(document: BlockNoteDocument): string {
    return JSON.stringify(document);
}

function isEmptyParagraphBlock(block: Record<string, unknown>): boolean {
    if (block.type !== 'paragraph') {
        return false;
    }

    const content = block.content;
    return !Array.isArray(content) || content.length === 0;
}

/** Strip trailing empty paragraphs BlockNote adds for editing. */
export function trimTrailingEmptyBlocks<T extends Record<string, unknown>>(blocks: readonly T[]): T[] {
    const result = [...blocks];

    while (result.length > 0 && isEmptyParagraphBlock(result[result.length - 1])) {
        result.pop();
    }

    return result;
}

export function normalizeTileTemplateForSave(document: BlockNoteDocument): BlockNoteDocument {
    return trimTrailingEmptyBlocks(document as Record<string, unknown>[]) as BlockNoteDocument;
}

export function parseTileTemplate(tileTemplate: string): BlockNoteDocument {
    if (!tileTemplate.trim()) {
        return DEFAULT_TILE_TEMPLATE;
    }

    try {
        const parsed = JSON.parse(tileTemplate) as BlockNoteDocument;
        if (Array.isArray(parsed)) {
            return parsed.slice(0, MAX_TILE_BLOCKS);
        }
    } catch {
        // fall through to default
    }

    return DEFAULT_TILE_TEMPLATE;
}
