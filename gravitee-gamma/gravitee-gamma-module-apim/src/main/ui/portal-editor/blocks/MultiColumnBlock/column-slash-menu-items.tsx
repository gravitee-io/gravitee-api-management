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
import type { schema } from '../schema';

import { insertColumnLayout } from './insert-column-layout';

type EditorType = typeof schema.BlockNoteEditor;

const columnsIcon = (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
        <rect x="3" y="3" width="7" height="18" rx="1" />
        <rect x="14" y="3" width="7" height="18" rx="1" />
    </svg>
);

const threeColumnsIcon = (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
        <rect x="2" y="3" width="5" height="18" rx="1" />
        <rect x="9.5" y="3" width="5" height="18" rx="1" />
        <rect x="17" y="3" width="5" height="18" rx="1" />
    </svg>
);

export function getColumnSlashMenuItems(editor: EditorType) {
    return [
        {
            title: 'Two Columns',
            onItemClick: () => insertColumnLayout(editor, 2),
            aliases: ['columns', '2 columns', 'two columns', 'column'],
            group: 'Multi-column',
            icon: columnsIcon,
            subtext: 'Insert two equal columns',
        },
        {
            title: 'Three Columns',
            onItemClick: () => insertColumnLayout(editor, 3),
            aliases: ['3 columns', 'three columns'],
            group: 'Multi-column',
            icon: threeColumnsIcon,
            subtext: 'Insert three equal columns',
        },
    ];
}
