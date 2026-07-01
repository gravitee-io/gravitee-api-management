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
import type { GammaBlock, GammaPartialBlock } from './gmd-types';

import { gmdToHtml } from './marked-gmd';
import {
    GMD_BLOCK_TYPE_TO_TAG,
    GMD_TAG_NAME_TO_BLOCK_TYPE,
    isFormGmdTag,
    parseGmdElementToPartialBlock,
} from './gmd-tag-registry';
import { splitGmdDocument } from './gmd-segments';
import { parseFirstElement } from './gmd-utils';

/** Minimal editor surface for HTML/markdown import; intentionally loose to accept BlockNoteEditor. */
type GmdParseEditor = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tryParseHTMLToBlocks: (html: string) => any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tryParseMarkdownToBlocks: (markdown: string) => any;
};

function parseHtmlElementToPartialBlock(outerHtml: string): GammaPartialBlock | undefined {
    const el = parseFirstElement(outerHtml);
    if (!el) {
        return undefined;
    }

    const tagName = el.tagName.toLowerCase();

    if (tagName === 'style') {
        return {
            type: 'graviteeHtml',
            props: {
                html: '',
                css: el.textContent ?? '',
            },
        };
    }

    if (tagName === 'img') {
        return {
            type: 'image',
            props: {
                url: el.getAttribute('src') ?? '',
                caption: el.getAttribute('title') ?? el.getAttribute('alt') ?? '',
                name: el.getAttribute('alt') ?? '',
            },
        };
    }

    if (tagName.startsWith('gmd-')) {
        return parseGmdElementFromHtml(outerHtml);
    }

    return {
        type: 'graviteeHtml',
        props: {
            html: outerHtml,
            css: '',
        },
    };
}

function parseGmdChildren(
    parentEl: HTMLElement,
    editor: GmdParseEditor,
    parseBlocks: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock[] {
    const childElements = Array.from(parentEl.children);
    if (childElements.length === 0) {
        const innerText = parentEl.innerHTML.trim();
        if (!innerText) {
            return [];
        }
        return parseBlocks(innerText);
    }

    const blocks: GammaPartialBlock[] = [];
    for (const child of childElements) {
        const parsed = parseGmdElementFromHtml(child.outerHTML, editor, parseBlocks);
        if (parsed) {
            blocks.push(parsed);
        }
    }

    if (blocks.length === 0) {
        return parseBlocks(parentEl.innerHTML);
    }

    return blocks;
}

function parseGmdElementFromHtml(
    outerHtml: string,
    editor?: GmdParseEditor,
    parseBlocks?: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock | undefined {
    const el = parseFirstElement(outerHtml);
    if (!el) {
        return undefined;
    }

    const tagName = el.tagName.toLowerCase();
    const blockType = GMD_TAG_NAME_TO_BLOCK_TYPE[tagName];

    if (!blockType) {
        if (isFormGmdTag(tagName)) {
            return {
                type: 'graviteeHtml',
                props: {
                    html: outerHtml,
                    css: '',
                },
            };
        }
        return undefined;
    }

    const mapping = GMD_BLOCK_TYPE_TO_TAG[blockType];

    if (mapping.hasChildren && editor && parseBlocks) {
        if (blockType === 'columnList') {
            const cellElements = Array.from(el.querySelectorAll(':scope > gmd-cell'));
            if (cellElements.length > 0) {
                const columns = cellElements.map(cell =>
                    ({
                        type: 'column',
                        props: {
                            width: Number.parseFloat(cell.getAttribute('width') ?? '1') || 1,
                        },
                        children: parseGmdChildren(cell as HTMLElement, editor, parseBlocks),
                    }) as GammaPartialBlock,
                );

                return {
                    type: 'columnList',
                    children: columns,
                } as GammaPartialBlock;
            }

            const directChildren = Array.from(el.children);
            if (directChildren.length > 0) {
                const columns = directChildren.map(child => ({
                    type: 'column' as const,
                    props: { width: 1 },
                    children:
                        child.tagName.toLowerCase().startsWith('gmd-') || child.tagName.toLowerCase() === 'img'
                            ? (() => {
                                  const parsed = parseGmdElementFromHtml(child.outerHTML, editor, parseBlocks)
                                      ?? parseHtmlElementToPartialBlock(child.outerHTML);
                                  return parsed ? [parsed] : parseBlocks(child.outerHTML);
                              })()
                            : parseBlocks(child.outerHTML),
                }));

                return {
                    type: 'columnList',
                    children: columns,
                } as GammaPartialBlock;
            }

            return {
                type: 'columnList',
                children: [{ type: 'column', props: { width: 1 }, children: parseBlocks(el.innerHTML) }],
            } as GammaPartialBlock;
        }

        const children = parseGmdChildren(el, editor, parseBlocks);
        return parseGmdElementToPartialBlock(el, children);
    }

    return parseGmdElementToPartialBlock(el);
}

function parseGmdSegments(gmd: string, editor: GmdParseEditor): GammaPartialBlock[] {
    const parseBlocks = (content: string) => parseGmdSegments(content, editor);
    const segments = splitGmdDocument(gmd);
    const blocks: GammaPartialBlock[] = [];

    for (const segment of segments) {
        if (segment.type === 'markdown') {
            blocks.push(...(editor.tryParseMarkdownToBlocks(segment.content) as GammaPartialBlock[]));
            continue;
        }

        const parsed = parseGmdElementFromHtml(segment.outerHtml, editor, parseBlocks)
            ?? parseHtmlElementToPartialBlock(segment.outerHtml);

        if (parsed) {
            blocks.push(parsed);
            continue;
        }

        const html = gmdToHtml(segment.outerHtml);
        blocks.push(...(editor.tryParseHTMLToBlocks(html) as GammaPartialBlock[]));
    }

    return blocks;
}

export function gmdToBlocks(gmd: string, editor: GmdParseEditor): GammaBlock[] {
    const trimmed = gmd.trim();
    if (!trimmed) {
        return [];
    }

    return parseGmdSegments(trimmed, editor) as GammaBlock[];
}

export function gmdToPartialBlocks(gmd: string, editor: GmdParseEditor): GammaPartialBlock[] {
    const trimmed = gmd.trim();
    if (!trimmed) {
        return [];
    }

    return parseGmdSegments(trimmed, editor);
}
