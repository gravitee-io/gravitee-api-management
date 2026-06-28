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

export function parseTileTemplate(tileTemplate: string): BlockNoteDocument {
    if (!tileTemplate.trim()) {
        return DEFAULT_TILE_TEMPLATE;
    }

    try {
        const parsed = JSON.parse(tileTemplate) as BlockNoteDocument;
        if (Array.isArray(parsed) && parsed.length > 0) {
            return parsed;
        }
    } catch {
        // fall through to default
    }

    return DEFAULT_TILE_TEMPLATE;
}
