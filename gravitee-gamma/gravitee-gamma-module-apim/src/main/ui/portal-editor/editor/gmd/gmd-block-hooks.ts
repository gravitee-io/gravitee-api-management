import { createElement } from 'react';
import type { FC, ReactNode } from 'react';

import {
    GMD_BLOCK_TYPE_TO_TAG,
    parseGmdElementToPartialBlock,
    buildGmdTag,
    type GmdTagMapping,
} from './gmd-tag-registry';
import { attrsToString } from './gmd-utils';

type BlockProps = {
    block: {
        type: string;
        props: Record<string, unknown>;
        children?: unknown[];
    };
};

function createToExternalHTML(mapping: GmdTagMapping): FC<BlockProps> {
    return ({ block }) => {
        const props = block.props;
        if (mapping.selfClosing) {
            const attrs = attrsToString(mapping.propsToAttrs(props));
            return createElement(mapping.tagName, parseAttrString(attrs));
        }

        if (mapping.contentProp) {
            const attrs = attrsToString(mapping.propsToAttrs(props));
            const content = String(props[mapping.contentProp] ?? '');
            return createElement(mapping.tagName, parseAttrString(attrs), content);
        }

        const tagHtml = buildGmdTag(mapping, props);
        return createElement('div', { dangerouslySetInnerHTML: { __html: tagHtml } });
    };
}

function parseAttrString(attrString: string): Record<string, string> {
    const attrs: Record<string, string> = {};
    const pattern = /([a-zA-Z0-9_-]+)="([^"]*)"/g;
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(attrString)) !== null) {
        attrs[match[1]] = match[2];
    }
    return attrs;
}

function createParseHandler(mapping: GmdTagMapping) {
    return (el: HTMLElement) => {
        if (el.tagName.toLowerCase() !== mapping.tagName) {
            return undefined;
        }
        return mapping.attrsToProps(el);
    };
}

export function getGmdBlockHooks(blockType: string): {
    parse?: (el: HTMLElement) => Record<string, unknown> | undefined;
    toExternalHTML?: FC<BlockProps & { context?: { nestingLevel: number }; children?: ReactNode }>;
} {
    const mapping = GMD_BLOCK_TYPE_TO_TAG[blockType];
    if (!mapping) {
        return {};
    }

    return {
        parse: createParseHandler(mapping),
        toExternalHTML: createToExternalHTML(mapping),
    };
}

export function parseLegacyGmdCard(el: HTMLElement): Record<string, unknown> | undefined {
    if (el.tagName.toLowerCase() !== 'gmd-card') {
        return undefined;
    }
    const block = parseGmdElementToPartialBlock(el);
    return block?.props as Record<string, unknown> | undefined;
}
