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
import type { BlockNoteDocument } from '../../portals/types';
import { looksLikeMarkdown, markdownToBlocks } from './markdown-to-blocks';

function getBlockPlainText(block: Record<string, unknown>): string {
    const content = block.content;
    if (!Array.isArray(content)) {
        return '';
    }

    return content
        .map(node =>
            typeof node === 'object' && node !== null && 'text' in node ? String((node as { text?: string }).text ?? '') : '',
        )
        .join('');
}

export function upgradeLegacyMarkdownInDocument(document: BlockNoteDocument): BlockNoteDocument {
    const upgraded: Record<string, unknown>[] = [];
    let changed = false;

    for (const block of document) {
        const record = block as Record<string, unknown>;
        if (record.type === 'paragraph') {
            const text = getBlockPlainText(record);
            if (looksLikeMarkdown(text)) {
                upgraded.push(...(markdownToBlocks(text) as Record<string, unknown>[]));
                changed = true;
                continue;
            }
        }

        upgraded.push(record);
    }

    return changed ? upgraded : document;
}
