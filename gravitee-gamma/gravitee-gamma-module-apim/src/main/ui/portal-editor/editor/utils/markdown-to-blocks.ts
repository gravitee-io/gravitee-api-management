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

import { schema } from '../../blocks/schema';

export { looksLikeMarkdown } from './looks-like-markdown';

export type PartialBlock = typeof schema.PartialBlock;

function createParserEditor() {
    return BlockNoteEditor.create({ schema });
}

let parserEditor: ReturnType<typeof createParserEditor> | undefined;

function getParserEditor() {
    parserEditor ??= createParserEditor();
    return parserEditor;
}

/**
 * Converts a Markdown string into BlockNote blocks using the portal gamma schema.
 */
export function markdownToBlocks(markdown: string): PartialBlock[] {
    return getParserEditor().tryParseMarkdownToBlocks(markdown);
}
