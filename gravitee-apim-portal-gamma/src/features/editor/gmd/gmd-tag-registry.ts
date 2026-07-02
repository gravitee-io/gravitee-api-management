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
import type { GammaPartialBlock } from './gmd-types';

import {
    attrsToString,
    decodeBase64,
    encodeBase64,
    getElementAttributes,
    getGmdMarkdownContent,
    mapBackgroundColorToCardColor,
} from './gmd-utils';

export interface GmdTagMapping {
    readonly tagName: string;
    readonly blockType: string;
    readonly hasChildren?: boolean;
    readonly contentProp?: string;
    readonly selfClosing?: boolean;
    readonly propsToAttrs: (props: Record<string, unknown>, childrenCount?: number) => Record<string, string>;
    readonly attrsToProps: (el: HTMLElement) => Record<string, unknown>;
    readonly serializeInner?: (props: Record<string, unknown>) => string;
}

const FORM_GMD_TAGS = [
    'gmd-input',
    'gmd-select',
    'gmd-radio',
    'gmd-checkbox',
    'gmd-textarea',
    'gmd-checkbox-group',
] as const;

export const CUSTOM_GMD_BLOCK_TYPES = new Set([
    'graviteeButton',
    'graviteeCard',
    'graviteeBanner',
    'graviteeSection',
    'graviteeContainer',
    'columnList',
    'column',
    'graviteeHtml',
    'graviteeMarkdown',
    'graviteeApiCatalog',
    'graviteeApiMetadata',
    'graviteeSubscriptionFlow',
    'graviteeSubscriptionViewer',
    'graviteeApplications',
    'graviteeInstallMcp',
]);

function stringProp(props: Record<string, unknown>, key: string, fallback = ''): string {
    const value = props[key];
    return typeof value === 'string' ? value : fallback;
}

function attrsFromProps(props: Record<string, unknown>, keys: string[]): Record<string, string> {
    const attrs: Record<string, string> = {};
    for (const key of keys) {
        const value = stringProp(props, key);
        if (value) {
            attrs[key] = value;
        }
    }
    return attrs;
}

function propsFromAttrs(el: HTMLElement, keys: string[]): Record<string, unknown> {
    const props: Record<string, unknown> = {};
    for (const key of keys) {
        props[key] = el.getAttribute(key) ?? '';
    }
    return props;
}

function parseCardFromElement(el: HTMLElement): Record<string, unknown> {
    const titleEl = el.querySelector('gmd-card-title');
    const subtitleEl = el.querySelector('gmd-card-subtitle');
    const backgroundColor = el.getAttribute('backgroundColor') ?? el.getAttribute('backgroundcolor');
    const color = el.getAttribute('color') ?? mapBackgroundColorToCardColor(backgroundColor);

    return {
        title: titleEl?.textContent?.trim() || el.getAttribute('title') || 'Feature Card',
        subtitle:
            subtitleEl?.textContent?.trim() ||
            el.getAttribute('subtitle') ||
            'Describe your feature or category here.',
        icon: el.getAttribute('icon') || 'book',
        color,
    };
}

function serializeCard(props: Record<string, unknown>): Record<string, string> {
    return attrsFromProps(props, ['color', 'icon', 'title', 'subtitle']);
}

function parseButtonFromElement(el: HTMLElement): Record<string, unknown> {
    return {
        label: el.textContent?.trim() || 'Get Started',
        link: el.getAttribute('link') || '/catalog',
        appearance: el.getAttribute('appearance') || 'filled',
    };
}

function serializeButton(props: Record<string, unknown>): Record<string, string> {
    return attrsFromProps(props, ['link', 'appearance']);
}

function parseMarkdownFromElement(el: HTMLElement): Record<string, unknown> {
    return {
        markdown: getGmdMarkdownContent(el),
    };
}

function serializeMarkdown(props: Record<string, unknown>): Record<string, string> {
    return {};
}

function parseHtmlFromElement(el: HTMLElement): Record<string, unknown> {
    const htmlAttr = el.getAttribute('html');
    const cssAttr = el.getAttribute('css');
    return {
        html: htmlAttr ? decodeBase64(htmlAttr) : '',
        css: cssAttr ? decodeBase64(cssAttr) : '',
    };
}

function serializeHtml(props: Record<string, unknown>): Record<string, string> {
    const html = stringProp(props, 'html');
    const css = stringProp(props, 'css');
    const attrs: Record<string, string> = {};
    if (html) {
        attrs.html = encodeBase64(html);
    }
    if (css) {
        attrs.css = encodeBase64(css);
    }
    return attrs;
}

function parseInstallMcpFromElement(el: HTMLElement): Record<string, unknown> {
    return propsFromAttrs(el, ['name', 'transport', 'url', 'headers', 'command', 'args', 'env', 'clients']);
}

function serializeInstallMcp(props: Record<string, unknown>): Record<string, string> {
    return attrsFromProps(props, ['name', 'transport', 'url', 'headers', 'command', 'args', 'env', 'clients']);
}

export const GMD_TAG_MAPPINGS: Record<string, GmdTagMapping> = {
    graviteeButton: {
        tagName: 'gmd-button',
        blockType: 'graviteeButton',
        contentProp: 'label',
        propsToAttrs: serializeButton,
        attrsToProps: parseButtonFromElement,
    },
    graviteeCard: {
        tagName: 'gmd-card',
        blockType: 'graviteeCard',
        propsToAttrs: serializeCard,
        attrsToProps: parseCardFromElement,
    },
    graviteeBanner: {
        tagName: 'gmd-banner',
        blockType: 'graviteeBanner',
        propsToAttrs: props =>
            attrsFromProps(props, ['title', 'subtitle', 'variant', 'height', 'backgroundImage', 'buttons']),
        attrsToProps: el => propsFromAttrs(el, ['title', 'subtitle', 'variant', 'height', 'backgroundImage', 'buttons']),
    },
    graviteeSection: {
        tagName: 'gmd-section',
        blockType: 'graviteeSection',
        propsToAttrs: props =>
            attrsFromProps(props, ['title', 'subtitle', 'variant', 'columns', 'height', 'contentWidth', 'items']),
        attrsToProps: el =>
            propsFromAttrs(el, ['title', 'subtitle', 'variant', 'columns', 'height', 'contentWidth', 'items']),
    },
    graviteeContainer: {
        tagName: 'gmd-container',
        blockType: 'graviteeContainer',
        hasChildren: true,
        propsToAttrs: props => attrsFromProps(props, ['variant', 'padding']),
        attrsToProps: el => propsFromAttrs(el, ['variant', 'padding']),
    },
    columnList: {
        tagName: 'gmd-grid',
        blockType: 'columnList',
        hasChildren: true,
        propsToAttrs: (props, childrenCount?: number) => ({
            columns: String(childrenCount ?? (Number.parseInt(stringProp(props, 'columns', '2'), 10) || 2)),
            ...attrsFromProps(props, ['class']),
        }),
        attrsToProps: el => ({
            columns: el.getAttribute('columns') ?? '',
            class: el.getAttribute('class') ?? '',
        }),
    },
    column: {
        tagName: 'gmd-cell',
        blockType: 'column',
        hasChildren: true,
        propsToAttrs: props => attrsFromProps(props, ['width']),
        attrsToProps: el => ({
            width: Number.parseFloat(el.getAttribute('width') ?? el.getAttribute('data-width') ?? '1') || 1,
        }),
    },
    graviteeHtml: {
        tagName: 'gmd-html',
        blockType: 'graviteeHtml',
        propsToAttrs: serializeHtml,
        attrsToProps: parseHtmlFromElement,
    },
    graviteeMarkdown: {
        tagName: 'gmd-md',
        blockType: 'graviteeMarkdown',
        contentProp: 'markdown',
        propsToAttrs: serializeMarkdown,
        attrsToProps: parseMarkdownFromElement,
    },
    graviteeApiCatalog: {
        tagName: 'gmd-api-catalog',
        blockType: 'graviteeApiCatalog',
        propsToAttrs: props => attrsFromProps(props, ['title', 'viewMode', 'tileTemplate']),
        attrsToProps: el => propsFromAttrs(el, ['title', 'viewMode', 'tileTemplate']),
    },
    graviteeApiMetadata: {
        tagName: 'gmd-api-metadata',
        blockType: 'graviteeApiMetadata',
        selfClosing: true,
        propsToAttrs: props => attrsFromProps(props, ['field']),
        attrsToProps: el => propsFromAttrs(el, ['field']),
    },
    graviteeSubscriptionFlow: {
        tagName: 'gmd-subscription-flow',
        blockType: 'graviteeSubscriptionFlow',
        propsToAttrs: props => attrsFromProps(props, ['apiId']),
        attrsToProps: el => propsFromAttrs(el, ['apiId']),
    },
    graviteeSubscriptionViewer: {
        tagName: 'gmd-subscription-viewer',
        blockType: 'graviteeSubscriptionViewer',
        propsToAttrs: () => ({}),
        attrsToProps: () => ({}),
    },
    graviteeApplications: {
        tagName: 'gmd-applications',
        blockType: 'graviteeApplications',
        propsToAttrs: () => ({}),
        attrsToProps: () => ({}),
    },
    graviteeInstallMcp: {
        tagName: 'gmd-install-mcp',
        blockType: 'graviteeInstallMcp',
        selfClosing: true,
        propsToAttrs: serializeInstallMcp,
        attrsToProps: parseInstallMcpFromElement,
    },
};

export const GMD_TAG_NAME_TO_BLOCK_TYPE = Object.fromEntries(
    Object.values(GMD_TAG_MAPPINGS).map(mapping => [mapping.tagName, mapping.blockType]),
);

export const GMD_BLOCK_TYPE_TO_TAG = Object.fromEntries(
    Object.values(GMD_TAG_MAPPINGS).map(mapping => [mapping.blockType, mapping]),
);

export function isCustomGmdBlockType(type: string): boolean {
    return CUSTOM_GMD_BLOCK_TYPES.has(type);
}

export function isFormGmdTag(tagName: string): boolean {
    return (FORM_GMD_TAGS as readonly string[]).includes(tagName.toLowerCase());
}

export function buildGmdTag(
    mapping: GmdTagMapping,
    props: Record<string, unknown>,
    innerContent = '',
    childrenCount?: number,
): string {
    const attrs = mapping.propsToAttrs(props, childrenCount);
    const attrString = attrsToString(attrs);

    if (mapping.selfClosing) {
        return attrString ? `<${mapping.tagName} ${attrString} />` : `<${mapping.tagName} />`;
    }

    if (mapping.contentProp) {
        const content = stringProp(props, mapping.contentProp);
        return attrString
            ? `<${mapping.tagName} ${attrString}>${content}</${mapping.tagName}>`
            : `<${mapping.tagName}>${content}</${mapping.tagName}>`;
    }

    const openTag = attrString ? `<${mapping.tagName} ${attrString}>` : `<${mapping.tagName}>`;
    if (!innerContent.trim()) {
        return `${openTag}</${mapping.tagName}>`;
    }
    return `${openTag}\n\n${innerContent.trim()}\n\n</${mapping.tagName}>`;
}

export function parseGmdElementToPartialBlock(
    el: HTMLElement,
    children: GammaPartialBlock[] = [],
): GammaPartialBlock | undefined {
    const tagName = el.tagName.toLowerCase();
    const blockType = GMD_TAG_NAME_TO_BLOCK_TYPE[tagName];

    if (!blockType) {
        if (isFormGmdTag(tagName)) {
            return {
                type: 'graviteeHtml',
                props: {
                    html: el.outerHTML,
                    css: '',
                },
            };
        }
        return undefined;
    }

    const mapping = GMD_BLOCK_TYPE_TO_TAG[blockType];
    const props = mapping.attrsToProps(el);

    if (mapping.contentProp && mapping.contentProp in props === false) {
        props[mapping.contentProp] = el.textContent?.trim() ?? '';
    }

    if (blockType === 'graviteeMarkdown' && !stringProp(props, 'markdown')) {
        props.markdown = el.textContent?.trim() ?? '';
    }

    const block = {
        type: blockType,
        props,
        ...(mapping.hasChildren && children.length > 0 ? { children } : {}),
    } as GammaPartialBlock;

    return block;
}
