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
import { BlockNoteEditor } from '@blocknote/core';

import { schema } from '../../../blocks/schema';
import type { BlockNoteDocument } from '../../portals/types';
import { gmdToPartialBlocks } from './gmd-parser';
import { blocksToGmd } from './gmd-serializer';
import type { GammaPartialBlock } from './gmd-types';

function createHeadlessEditor() {
    return BlockNoteEditor.create({ schema });
}

type HeadlessEditor = ReturnType<typeof createHeadlessEditor>;

let headlessEditor: HeadlessEditor | undefined;

function getHeadlessEditor(): HeadlessEditor {
    headlessEditor ??= createHeadlessEditor();
    return headlessEditor;
}

export function parseGmdToDocument(gmd: string): BlockNoteDocument {
    const editor = getHeadlessEditor();
    return gmdToPartialBlocks(gmd, editor) as unknown as BlockNoteDocument;
}

export function serializeDocumentToGmd(document: BlockNoteDocument): string {
    const editor = getHeadlessEditor();
    return blocksToGmd(document as unknown as GammaPartialBlock[], editor);
}

export function resolveBlockPageDocument(
    document: BlockNoteDocument,
    gmd?: string,
): BlockNoteDocument {
    if (gmd?.trim()) {
        return parseGmdToDocument(gmd);
    }
    return document;
}
