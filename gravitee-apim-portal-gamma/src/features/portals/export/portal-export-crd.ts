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
import yaml from 'js-yaml';

import type {
    AsyncApiPageContent,
    BlockPageContent,
    HtmlPageContent,
    OpenApiPageContent,
    PageContent,
    PortalNavigationApi,
    PortalNavigationItem,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../types';
import {
    getPageContentType,
    isAsyncApiPage,
    isBlockPageContent,
    isOpenApiPage,
} from '../utils/page-content-type';
import { aggregatePortalExport } from './aggregate-portal-export';
import { buildNavPaths, getParentPath } from './build-nav-paths';
import { derivePortalHrid, deriveResourceName } from './derive-portal-hrid';
import { downloadFile } from './download-file';
import { exportPageToMarkup } from './export-page-markup';
import type { K8sResourceDocument, PortalExportBundle } from './portal-export.types';

const API_VERSION = 'gravitee.io/v1alpha1';

function createDocument(kind: string, name: string, spec: Record<string, unknown>): K8sResourceDocument {
    return {
        apiVersion: API_VERSION,
        kind,
        metadata: { name: deriveResourceName(name) },
        spec,
    };
}

function buildNavigationEntries(
    navItems: readonly PortalNavigationItem[],
    paths: ReadonlyMap<string, string>,
): Record<string, unknown>[] {
    return navItems.map(item => {
        const entry: Record<string, unknown> = {
            path: paths.get(item.id) ?? `/${item.slug}`,
            displayName: item.title,
            order: item.order,
            type: item.type,
        };

        if (item.area) {
            entry.area = item.area;
        }

        if (item.type === 'LINK') {
            entry.url = (item as PortalNavigationLink).url;
        }

        return entry;
    });
}

function derivePageHrid(page: PortalNavigationPage, location: string): string {
    const fromSlug = page.slug.replace(/-nav[a-z0-9-]*$/i, '');
    if (fromSlug) {
        return deriveResourceName(fromSlug);
    }
    return deriveResourceName(location.replace(/^\//, '').replace(/\//g, '-') || page.title);
}

function resolveOpenApiContent(
    page: PortalNavigationPage,
    content: OpenApiPageContent,
): { content: string; specSource?: Record<string, unknown> } {
    if (isOpenApiPage(page) && page.specSource.type === 'INLINE') {
        return { content: page.specSource.content, specSource: page.specSource };
    }
    if (isOpenApiPage(page) && page.specSource.type === 'URL') {
        return {
            content: content.specContent,
            specSource: page.specSource,
        };
    }
    if (isOpenApiPage(page) && page.specSource.type === 'API') {
        return {
            content: content.specContent,
            specSource: page.specSource,
        };
    }
    return { content: content.specContent };
}

function resolveAsyncApiContent(
    page: PortalNavigationPage,
    content: AsyncApiPageContent,
): { content: string; specSource?: Record<string, unknown> } {
    if (isAsyncApiPage(page) && page.specSource.type === 'INLINE') {
        return { content: page.specSource.content, specSource: page.specSource };
    }
    if (isAsyncApiPage(page)) {
        return {
            content: content.specContent,
            specSource: page.specSource,
        };
    }
    return { content: content.specContent };
}

export function buildPortalCrdDocuments(bundle: PortalExportBundle): K8sResourceDocument[] {
    const hrid = derivePortalHrid(bundle.portal);
    const paths = buildNavPaths(bundle.navigation);
    const documents: K8sResourceDocument[] = [];

    documents.push(
        createDocument('Portal', hrid, {
            hrid,
            name: bundle.portal.name,
            layout: bundle.portal.layout,
            portalIconUrl: bundle.portal.portalIconUrl,
            portalLabel: bundle.portal.portalLabel,
            footerLinks: bundle.portal.footerLinks,
            userMenuItems: bundle.portal.userMenuItems,
            navigation: buildNavigationEntries(bundle.navigation, paths),
        }),
    );

    const apiItems = bundle.navigation.filter((item): item is PortalNavigationApi => item.type === 'API');
    if (apiItems.length > 0) {
        documents.push(
            createDocument('PortalListing', `${hrid}-listing`, {
                hrid: `${hrid}-listing`,
                apis: apiItems.map(item => ({
                    apiHrid: item.apiId,
                    location: getParentPath(item, paths),
                    order: item.order,
                })),
            }),
        );
    }

    const pageContentsByNavId = new Map(
        bundle.pageContents.map(content => [content.navigationItemId, content]),
    );

    for (const navItem of bundle.navigation) {
        if (navItem.type !== 'PAGE') {
            continue;
        }

        const page = navItem;
        const content = pageContentsByNavId.get(page.id);
        if (!content) {
            continue;
        }

        const location = paths.get(page.id) ?? `/${page.slug}`;
        const pageHrid = derivePageHrid(page, location);
        const contentType = getPageContentType(page);

        if (contentType === 'BLOCK' && isBlockPageContent(content)) {
            documents.push(buildBlockPageDocument(hrid, pageHrid, location, page, content));
            documents.push(buildPortalPageDocument(hrid, pageHrid, location, page, content));
            continue;
        }

        if (contentType === 'OPENAPI' && content.contentType === 'OPENAPI') {
            documents.push(buildOpenApiDocumentationDocument(hrid, pageHrid, location, page, content));
            continue;
        }

        if (contentType === 'ASYNCAPI' && content.contentType === 'ASYNCAPI') {
            documents.push(buildAsyncApiDocumentationDocument(hrid, pageHrid, location, page, content));
            continue;
        }

        if (contentType === 'HTML' && content.contentType === 'HTML') {
            documents.push(buildHtmlDocumentationDocument(hrid, pageHrid, location, page, content));
        }
    }

    documents.push(
        createDocument('PortalTheme', `${hrid}-theme`, {
            portalHrid: hrid,
            schemaVersion: bundle.theme.schemaVersion ?? 1,
            activeMode: bundle.theme.activeMode,
            foundation: bundle.theme.foundation,
            elements: bundle.theme.elements,
            customVariables: bundle.theme.customVariables,
        }),
    );

    return documents;
}

function buildBlockPageDocument(
    portalHrid: string,
    pageHrid: string,
    location: string,
    page: PortalNavigationPage,
    content: BlockPageContent,
): K8sResourceDocument {
    const spec: Record<string, unknown> = {
        portalHrid,
        hrid: pageHrid,
        name: page.title,
        location,
        order: page.order,
        document: content.document,
    };

    return createDocument('PortalBlockPage', `${portalHrid}-${pageHrid}`, spec);
}

function buildPortalPageDocument(
    portalHrid: string,
    pageHrid: string,
    location: string,
    page: PortalNavigationPage,
    content: BlockPageContent,
): K8sResourceDocument {
    return createDocument('PortalPage', `${portalHrid}-${pageHrid}-markup`, {
        portalHrid,
        hrid: pageHrid,
        name: page.title,
        location,
        order: page.order,
        content: exportPageToMarkup(content),
    });
}

function buildOpenApiDocumentationDocument(
    portalHrid: string,
    pageHrid: string,
    location: string,
    page: PortalNavigationPage,
    content: OpenApiPageContent,
): K8sResourceDocument {
    const resolved = resolveOpenApiContent(page, content);
    const spec: Record<string, unknown> = {
        portalHrid,
        hrid: pageHrid,
        name: page.title,
        type: 'OPENAPI',
        content: resolved.content,
        location,
        order: page.order,
        renderer: content.renderer,
    };

    if (resolved.specSource) {
        spec.specSource = resolved.specSource;
    }

    return createDocument('PortalDocumentation', `${portalHrid}-${pageHrid}`, spec);
}

function buildAsyncApiDocumentationDocument(
    portalHrid: string,
    pageHrid: string,
    location: string,
    page: PortalNavigationPage,
    content: AsyncApiPageContent,
): K8sResourceDocument {
    const resolved = resolveAsyncApiContent(page, content);
    const spec: Record<string, unknown> = {
        portalHrid,
        hrid: pageHrid,
        name: page.title,
        type: 'ASYNCAPI',
        content: resolved.content,
        location,
        order: page.order,
    };

    if (resolved.specSource) {
        spec.specSource = resolved.specSource;
    }

    return createDocument('PortalDocumentation', `${portalHrid}-${pageHrid}`, spec);
}

function buildHtmlDocumentationDocument(
    portalHrid: string,
    pageHrid: string,
    location: string,
    page: PortalNavigationPage,
    content: HtmlPageContent,
): K8sResourceDocument {
    return createDocument('PortalDocumentation', `${portalHrid}-${pageHrid}`, {
        portalHrid,
        hrid: pageHrid,
        name: page.title,
        type: 'HTML',
        content: content.html,
        ...(content.css ? { css: content.css } : {}),
        location,
        order: page.order,
    });
}

export function exportPortalToCrdsYaml(bundle: PortalExportBundle): string {
    const documents = buildPortalCrdDocuments(bundle);
    return documents
        .map(document => yaml.dump(document, { lineWidth: 120, noRefs: true }).trimEnd())
        .join('\n---\n')
        .concat('\n');
}

export async function downloadPortalCrds(portalId: string): Promise<void> {
    const bundle = await aggregatePortalExport(portalId);
    const hrid = derivePortalHrid(bundle.portal);
    const yamlContent = exportPortalToCrdsYaml(bundle);
    downloadFile(yamlContent, `portal-${hrid}.yaml`, 'application/x-yaml');
}
