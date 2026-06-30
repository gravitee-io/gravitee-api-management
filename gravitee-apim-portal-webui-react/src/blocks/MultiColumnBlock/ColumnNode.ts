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
import { Node } from '@tiptap/core';

export const ColumnNode = Node.create({
    name: 'column',
    group: 'bnBlock childContainer',
    content: 'blockContainer+',
    priority: 40,
    defining: true,
    marks: 'deletion insertion modification',

    addAttributes() {
        return {
            width: {
                default: 1,
                parseHTML: element => {
                    const attr = element.getAttribute('data-width');
                    if (attr === null) {
                        return null;
                    }

                    const parsed = Number.parseFloat(attr);
                    return Number.isFinite(parsed) ? parsed : null;
                },
                renderHTML: attributes => ({
                    'data-width': String(attributes.width as number),
                    style: `flex-grow: ${attributes.width as number};`,
                }),
            },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div',
                getAttrs: element => {
                    if (typeof element === 'string') {
                        return false;
                    }

                    return element.getAttribute('data-node-type') === this.name ? {} : false;
                },
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        const column = document.createElement('div');
        column.className = 'bn-block-column';
        column.setAttribute('data-node-type', this.name);

        for (const [attribute, value] of Object.entries(HTMLAttributes)) {
            if (value != null) {
                column.setAttribute(attribute, value as string);
            }
        }

        return {
            dom: column,
            contentDOM: column,
        };
    },
});
