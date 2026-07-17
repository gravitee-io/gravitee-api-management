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
import { createBlockSpecFromTiptapNode } from '@blocknote/core';

import { ColumnListNode } from './ColumnListNode';
import { ColumnNode } from './ColumnNode';

export const ColumnListBlock = createBlockSpecFromTiptapNode(
    {
        node: ColumnListNode,
        type: 'columnList',
        content: 'none',
    },
    {},
);

export const ColumnBlock = createBlockSpecFromTiptapNode(
    {
        node: ColumnNode,
        type: 'column',
        content: 'none',
    },
    {
        width: { default: 1 },
    },
);
