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

export type ClassicPageStatus = 'ready' | 'warning' | 'skipped';

export type ClassicPageType =
    | 'MARKDOWN'
    | 'SWAGGER'
    | 'ASYNCAPI'
    | 'ASCIIDOC'
    | 'FOLDER'
    | 'LINK'
    | 'TRANSLATION';

export interface ClassicPageNode {
    readonly id: string;
    readonly title: string;
    readonly type: ClassicPageType;
    readonly status: ClassicPageStatus;
    readonly reason?: string;
    readonly children?: readonly ClassicPageNode[];
    readonly homepage?: boolean;
    readonly linkUrl?: string;
}

export type ClassicConflictMode = 'replace' | 'merge';

export interface ClassicImportMappingOptions {
    readonly markdownToGmd: boolean;
    readonly swaggerToApiBlocks: boolean;
    readonly asyncApiPages: boolean;
    readonly externalLinks: boolean;
    readonly preserveFolders: boolean;
    readonly setHomepageAsOverview: boolean;
    readonly includeWarningPages: boolean;
}

export const DEFAULT_CLASSIC_IMPORT_MAPPING: ClassicImportMappingOptions = {
    markdownToGmd: true,
    swaggerToApiBlocks: true,
    asyncApiPages: true,
    externalLinks: false,
    preserveFolders: true,
    setHomepageAsOverview: true,
    includeWarningPages: false,
};

export const CLASSIC_IMPORT_LIMITATIONS = [
    'Translations (i18n pages) — imported as separate pages only',
    'Markdown templates / Freemarker variables — not resolved',
    'AsciiDoc pages — skipped (unsupported format)',
    'Git fetcher / auto-sync sources — content imported as static',
    'Attached media — files not copied',
    'Per-page access controls — not ported',
    'Swagger UI settings (Try-it, PKCE, doc expansion) — renderer choice only (Gravitee / Redoc / Swagger)',
    'Page revision history — not migrated',
] as const;

export function getClassicDocumentationFixture(_apiName: string): ClassicPageNode[] {
    return [
        {
            id: 'classic-overview',
            title: 'Overview',
            type: 'MARKDOWN',
            status: 'ready',
            homepage: true,
        },
        {
            id: 'classic-getting-started',
            title: 'Getting Started',
            type: 'MARKDOWN',
            status: 'ready',
        },
        {
            id: 'classic-openapi',
            title: 'OpenAPI Reference',
            type: 'SWAGGER',
            status: 'ready',
        },
        {
            id: 'classic-asyncapi',
            title: 'Events',
            type: 'ASYNCAPI',
            status: 'ready',
        },
        {
            id: 'classic-guides',
            title: 'Guides',
            type: 'FOLDER',
            status: 'ready',
            children: [
                {
                    id: 'classic-rate-limiting',
                    title: 'Rate limiting',
                    type: 'MARKDOWN',
                    status: 'ready',
                },
                {
                    id: 'classic-error-codes',
                    title: 'Error codes',
                    type: 'MARKDOWN',
                    status: 'ready',
                },
                {
                    id: 'classic-best-practices',
                    title: 'Best practices',
                    type: 'MARKDOWN',
                    status: 'ready',
                },
            ],
        },
        {
            id: 'classic-authentication',
            title: 'Authentication',
            type: 'MARKDOWN',
            status: 'warning',
            reason: 'Has attached media',
        },
        {
            id: 'classic-overview-fr',
            title: 'Overview (fr)',
            type: 'TRANSLATION',
            status: 'warning',
            reason: 'Translation page',
        },
        {
            id: 'classic-external-docs',
            title: 'External docs',
            type: 'LINK',
            status: 'skipped',
            reason: 'External link',
            linkUrl: 'https://docs.example.com/api',
        },
        {
            id: 'classic-asciidoc',
            title: 'Legacy AsciiDoc page',
            type: 'ASCIIDOC',
            status: 'skipped',
            reason: 'Unsupported format',
        },
    ];
}

export function countClassicPageNodes(nodes: readonly ClassicPageNode[]): number {
    return nodes.reduce((total, node) => {
        const childCount = node.children ? countClassicPageNodes(node.children) : 0;
        return total + 1 + childCount;
    }, 0);
}

export function flattenClassicPageNodes(nodes: readonly ClassicPageNode[]): ClassicPageNode[] {
    const result: ClassicPageNode[] = [];

    function walk(node: ClassicPageNode): void {
        result.push(node);
        node.children?.forEach(walk);
    }

    nodes.forEach(walk);
    return result;
}

export function isClassicPageSelectable(node: ClassicPageNode, includeWarningPages: boolean): boolean {
    if (node.type === 'FOLDER') {
        return false;
    }

    if (node.status === 'skipped') {
        return false;
    }

    if (node.status === 'warning') {
        return includeWarningPages;
    }

    return true;
}

export function getDefaultSelectedPageIds(
    nodes: readonly ClassicPageNode[],
    includeWarningPages = false,
): Set<string> {
    return new Set(
        flattenClassicPageNodes(nodes)
            .filter(node => isClassicPageSelectable(node, includeWarningPages))
            .map(node => node.id),
    );
}

export function getImportablePageIds(nodes: readonly ClassicPageNode[], includeWarningPages: boolean): string[] {
    return flattenClassicPageNodes(nodes)
        .filter(node => node.status === 'ready' || (includeWarningPages && node.status === 'warning'))
        .filter(node => node.type !== 'FOLDER')
        .map(node => node.id);
}

export function formatClassicPageLabel(node: ClassicPageNode): string {
    const typeLabel = node.type === 'SWAGGER' ? 'SWAGGER' : node.type;
    const homepageSuffix = node.homepage ? ', homepage' : '';
    const linkSuffix = node.linkUrl ? ` → ${node.linkUrl}` : '';
    const reasonSuffix = node.reason ? ` — ${node.reason}` : '';

    if (node.type === 'FOLDER') {
        const childCount = node.children?.length ?? 0;
        return `${node.title} (${typeLabel}) — ${childCount} ${childCount === 1 ? 'child' : 'children'}`;
    }

    return `${node.title} (${typeLabel}${homepageSuffix})${linkSuffix}${reasonSuffix}`;
}
