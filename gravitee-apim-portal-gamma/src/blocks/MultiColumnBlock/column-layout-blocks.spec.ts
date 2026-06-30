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
import { createColumnListBlock } from './column-layout-blocks';

describe('column-layout-blocks', () => {
    it('should create a two-column layout with paragraph children', () => {
        const block = createColumnListBlock(2);

        expect(block.type).toBe('columnList');
        expect(block.children).toHaveLength(2);
        expect(block.children[0]).toMatchObject({
            type: 'column',
            props: { width: 1 },
        });
        expect(block.children[0].children[0]).toMatchObject({ type: 'paragraph' });
        expect(block.children[1].children[0]).toMatchObject({ type: 'paragraph' });
    });

    it('should create a three-column layout', () => {
        const block = createColumnListBlock(3);

        expect(block.type).toBe('columnList');
        expect(block.children).toHaveLength(3);
        block.children.forEach(column => {
            expect(column).toMatchObject({
                type: 'column',
                props: { width: 1 },
            });
        });
    });
});
