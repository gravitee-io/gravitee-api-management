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
import {
    buildGmdTag,
    GMD_BLOCK_TYPE_TO_TAG,
    isCustomGmdBlockType,
} from './gmd-tag-registry';
import type { GammaPartialBlock } from './gmd-types';
import { isStyleOnlyHtmlBlock } from './gmd-utils';

/** Minimal editor surface for markdown export; intentionally loose to accept BlockNoteEditor. */
type GmdMarkdownEditor = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    blocksToMarkdownLossy: (blocks?: any) => string;
};

function isRawHtmlBlock(block: GammaPartialBlock): boolean {
    if (block.type !== 'graviteeHtml' || !block.props) {
        return false;
    }
    const html = typeof block.props.html === 'string' ? block.props.html : '';
    const css = typeof block.props.css === 'string' ? block.props.css : '';
    return Boolean(html.trim()) && !css.trim() && html.trim().startsWith('<');
}

export function serializeBlockToGmd(
    block: GammaPartialBlock,
    editor: GmdMarkdownEditor,
    serializeChildren: (children: GammaPartialBlock[]) => string,
): string {
    const blockType = String(block.type);

    if (!isCustomGmdBlockType(blockType)) {
        return editor.blocksToMarkdownLossy([block]).trim();
    }

    const mapping = GMD_BLOCK_TYPE_TO_TAG[blockType];
    if (!mapping) {
        return editor.blocksToMarkdownLossy([block]).trim();
    }

    const props = (block.props ?? {}) as Record<string, unknown>;

    if (blockType === 'graviteeHtml') {
        if (isStyleOnlyHtmlBlock(block)) {
            return `<style>\n${String(props.css ?? '').trim()}\n</style>`;
        }
        if (isRawHtmlBlock(block)) {
            return String(props.html ?? '').trim();
        }
    }

    if (mapping.hasChildren) {
        const children = (block.children ?? []) as GammaPartialBlock[];
        const innerContent = serializeChildren(children);
        const childrenCount = blockType === 'columnList' ? children.length : undefined;
        return buildGmdTag(mapping, props, innerContent, childrenCount);
    }

    return buildGmdTag(mapping, props);
}

export function blocksToGmd(blocks: GammaPartialBlock[], editor: GmdMarkdownEditor): string {
    const serializeChildren = (children: GammaPartialBlock[]) => blocksToGmd(children, editor);

    return blocks
        .map(block => serializeBlockToGmd(block, editor, serializeChildren))
        .filter(part => part.length > 0)
        .join('\n\n');
}
