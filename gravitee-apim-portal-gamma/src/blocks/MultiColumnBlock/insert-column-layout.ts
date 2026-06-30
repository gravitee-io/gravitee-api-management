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
import { insertOrUpdateBlockForSlashMenu } from '@blocknote/core/extensions';

import type { schema } from '../schema';

import { createColumnListBlock } from './column-layout-blocks';

type EditorType = typeof schema.BlockNoteEditor;
type PartialBlockType = typeof schema.PartialBlock;

export { createColumnListBlock } from './column-layout-blocks';

export function insertColumnLayout(editor: EditorType, columnCount: 2 | 3): void {
    insertOrUpdateBlockForSlashMenu(editor, createColumnListBlock(columnCount) as PartialBlockType);
}
