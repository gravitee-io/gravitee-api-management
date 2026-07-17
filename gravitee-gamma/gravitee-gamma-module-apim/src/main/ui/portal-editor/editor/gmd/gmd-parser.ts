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
import {
    elementContainsGmdDescendant,
    getGmdMarkdownContent,
    isGmdTagName,
    isHtmlContainerTag,
    mergeTrailingStyleBlocks,
    parseFirstElement,
} from './gmd-utils';

/** Block types that BlockNote allows inside column nodes. */
const COLUMN_COMPATIBLE_BLOCK_TYPES = new Set([
    'paragraph',
    'heading',
    'bulletListItem',
    'numberedListItem',
    'checkListItem',
    'codeBlock',
    'image',
    'divider',
    'table',
    'audio',
    'video',
    'file',
    'toggleListItem',
    'graviteeButton',
    'graviteeMarkdown',
    'graviteeCard',
    'graviteeHtml',
]);

type GmdParseResult = GammaPartialBlock | GammaPartialBlock[] | undefined;

/** Minimal editor surface for HTML/markdown import; intentionally loose to accept BlockNoteEditor. */
type GmdParseEditor = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tryParseHTMLToBlocks: (html: string) => any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tryParseMarkdownToBlocks: (markdown: string) => any;
};

function isColumnCompatibleBlock(block: GammaPartialBlock): boolean {
    const type = block.type;
    if (!type) {
        return false;
    }

    if (type === 'columnList' || type === 'column') {
        return (block.children ?? []).every(child => isColumnCompatibleBlock(child));
    }

    return COLUMN_COMPATIBLE_BLOCK_TYPES.has(type);
}

function appendParseResult(blocks: GammaPartialBlock[], result: GmdParseResult): void {
    if (!result) {
        return;
    }

    if (Array.isArray(result)) {
        blocks.push(...result);
        return;
    }

    blocks.push(result);
}

function htmlElementOnlyWrapsGmdContent(el: HTMLElement): boolean {
    const childElements = Array.from(el.children);
    if (childElements.length === 0) {
        return false;
    }

    return childElements.every(child => {
        const tagName = child.tagName.toLowerCase();
        if (isGmdTagName(tagName)) {
            return true;
        }

        return isHtmlContainerTag(tagName) && elementContainsGmdDescendant(child);
    });
}

function parseGmdMarkdownElement(
    el: HTMLElement,
    editor: GmdParseEditor,
    parseBlocks: (gmd: string) => GammaPartialBlock[],
): GmdParseResult {
    const markdown = getGmdMarkdownContent(el);
    if (!markdown) {
        return undefined;
    }

    return parseBlocks(markdown);
}

function parseGridChildToColumnBlocks(
    child: Element,
    editor: GmdParseEditor,
    parseBlocks: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock[] {
    const tagName = child.tagName.toLowerCase();
    if (isGmdTagName(tagName) || tagName === 'img') {
        const parsed = parseGmdElementFromHtml(child.outerHTML, editor, parseBlocks)
            ?? parseHtmlElementToPartialBlock(child.outerHTML, editor, parseBlocks);
        if (parsed) {
            return Array.isArray(parsed) ? parsed : [parsed];
        }
    }

    return parseMixedHtmlChildren(child as HTMLElement, editor, parseBlocks)
        ?? parseBlocks(child.outerHTML);
}

function flattenGmdGridChildren(
    el: HTMLElement,
    editor: GmdParseEditor,
    parseBlocks: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock[] {
    const directChildren = Array.from(el.children);
    if (directChildren.length === 0) {
        return parseBlocks(el.innerHTML);
    }

    const blocks: GammaPartialBlock[] = [];
    for (const child of directChildren) {
        if (child.tagName.toLowerCase() === 'gmd-cell') {
            blocks.push(...parseGmdChildren(child as HTMLElement, editor, parseBlocks));
            continue;
        }

        blocks.push(...parseGridChildToColumnBlocks(child, editor, parseBlocks));
    }

    return blocks;
}

function shouldFlattenGmdGrid(el: HTMLElement): boolean {
    const directChildren = Array.from(el.children);
    if (directChildren.length === 0) {
        return false;
    }

    const cellElements = directChildren.filter(child => child.tagName.toLowerCase() === 'gmd-cell');
    return cellElements.length !== directChildren.length;
}

function buildColumnListFromGmdGrid(
    el: HTMLElement,
    editor: GmdParseEditor,
    parseBlocks: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock {
    const directChildren = Array.from(el.children);
    const cellElements = directChildren.filter(child => child.tagName.toLowerCase() === 'gmd-cell');
    const useCellColumns = cellElements.length > 0 && cellElements.length === directChildren.length;
    const gridProps = parseGmdElementToPartialBlock(el)?.props ?? {};

    if (useCellColumns) {
        return {
            type: 'columnList',
            props: gridProps,
            children: cellElements.map(cell => ({
                type: 'column',
                props: {
                    width: Number.parseFloat(cell.getAttribute('width') ?? '1') || 1,
                },
                children: parseGmdChildren(cell as HTMLElement, editor, parseBlocks),
            })),
        } as GammaPartialBlock;
    }

    const columnsAttr = Number.parseInt(el.getAttribute('columns') ?? '', 10);
    const targetColumns = Number.isFinite(columnsAttr) && columnsAttr > 0 ? columnsAttr : directChildren.length;

    if (directChildren.length > 0) {
        return {
            type: 'columnList',
            props: gridProps,
            children: directChildren.map(child => ({
                type: 'column' as const,
                props: { width: 1 },
                children: parseGridChildToColumnBlocks(child, editor, parseBlocks),
            })),
        } as GammaPartialBlock;
    }

    const emptyColumns = Math.max(targetColumns, 1);
    return {
        type: 'columnList',
        props: gridProps,
        children: Array.from({ length: emptyColumns }, () => ({
            type: 'column',
            props: { width: 1 },
            children: parseBlocks(el.innerHTML),
        })),
    } as GammaPartialBlock;
}

function parseHtmlElementToPartialBlock(
    outerHtml: string,
    editor?: GmdParseEditor,
    parseBlocks?: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock | undefined {
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

    if (isGmdTagName(tagName)) {
        const parsed = parseGmdElementFromHtml(outerHtml, editor, parseBlocks);
        if (Array.isArray(parsed)) {
            return parsed[0];
        }
        return parsed;
    }

    if (editor && parseBlocks) {
        const mixed = parseMixedHtmlChildren(el, editor, parseBlocks);
        if (mixed?.length === 1) {
            return mixed[0];
        }
    }

    return {
        type: 'graviteeHtml',
        props: {
            html: outerHtml,
            css: '',
        },
    };
}

function parseMixedHtmlChildren(
    parentEl: HTMLElement,
    editor: GmdParseEditor,
    parseBlocks: (gmd: string) => GammaPartialBlock[],
): GammaPartialBlock[] | undefined {
    if (!elementContainsGmdDescendant(parentEl)) {
        return undefined;
    }

    const blocks: GammaPartialBlock[] = [];

    for (const child of Array.from(parentEl.childNodes)) {
        if (child.nodeType === Node.TEXT_NODE) {
            const text = (child.textContent ?? '').trim();
            if (text) {
                blocks.push(...parseBlocks(text));
            }
            continue;
        }

        if (child.nodeType !== Node.ELEMENT_NODE) {
            continue;
        }

        const childEl = child as HTMLElement;
        const tagName = childEl.tagName.toLowerCase();

        if (isGmdTagName(tagName)) {
            appendParseResult(blocks, parseGmdElementFromHtml(childEl.outerHTML, editor, parseBlocks));
            continue;
        }

        if (elementContainsGmdDescendant(childEl)) {
            if (htmlElementOnlyWrapsGmdContent(childEl)) {
                blocks.push(...parseGmdChildren(childEl, editor, parseBlocks));
                continue;
            }

            const nested = parseMixedHtmlChildren(childEl, editor, parseBlocks);
            if (nested) {
                blocks.push(...nested);
                continue;
            }
        }

        blocks.push({
            type: 'graviteeHtml',
            props: {
                html: childEl.outerHTML,
                css: '',
            },
        });
    }

    return blocks;
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
        const tagName = child.tagName.toLowerCase();

        if (tagName === 'gmd-md') {
            appendParseResult(blocks, parseGmdMarkdownElement(child as HTMLElement, editor, parseBlocks));
            continue;
        }

        if (!isGmdTagName(tagName) && elementContainsGmdDescendant(child)) {
            if (htmlElementOnlyWrapsGmdContent(child as HTMLElement)) {
                blocks.push(...parseGmdChildren(child as HTMLElement, editor, parseBlocks));
                continue;
            }

            const mixed = parseMixedHtmlChildren(child as HTMLElement, editor, parseBlocks);
            if (mixed) {
                blocks.push(...mixed);
                continue;
            }
        }

        const parsed = parseGmdElementFromHtml(child.outerHTML, editor, parseBlocks)
            ?? parseHtmlElementToPartialBlock(child.outerHTML, editor, parseBlocks);
        appendParseResult(blocks, parsed);
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
): GmdParseResult {
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

    if (blockType === 'graviteeMarkdown' && editor && parseBlocks) {
        return parseGmdMarkdownElement(el, editor, parseBlocks);
    }

    if (mapping.hasChildren && editor && parseBlocks) {
        if (blockType === 'columnList') {
            if (shouldFlattenGmdGrid(el)) {
                return flattenGmdGridChildren(el, editor, parseBlocks);
            }

            const columnList = buildColumnListFromGmdGrid(el, editor, parseBlocks);
            if (!isColumnCompatibleBlock(columnList)) {
                return flattenGmdGridChildren(el, editor, parseBlocks);
            }
            return columnList;
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

        const rootEl = parseFirstElement(segment.outerHtml);
        if (rootEl && isHtmlContainerTag(rootEl.tagName) && elementContainsGmdDescendant(rootEl)) {
            const mixed = parseMixedHtmlChildren(rootEl, editor, parseBlocks);
            if (mixed) {
                blocks.push(...mixed);
                continue;
            }
        }

        const parsed = parseGmdElementFromHtml(segment.outerHtml, editor, parseBlocks)
            ?? parseHtmlElementToPartialBlock(segment.outerHtml, editor, parseBlocks);

        appendParseResult(blocks, parsed);
        if (parsed) {
            continue;
        }

        const html = gmdToHtml(segment.outerHtml);
        blocks.push(...(editor.tryParseHTMLToBlocks(html) as GammaPartialBlock[]));
    }

    return mergeTrailingStyleBlocks(blocks);
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
