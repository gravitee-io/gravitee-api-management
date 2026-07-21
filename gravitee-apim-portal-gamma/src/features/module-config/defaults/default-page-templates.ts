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
import type { PageContentType } from '../../portals/types';
import type { PageTemplate, PageTemplateKind } from '../types';

export interface DefaultPageTemplateDefinition {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly contentType: PageContentType;
    readonly kind: PageTemplateKind;
    readonly bodyStub: string;
}

export const DEFAULT_PAGE_TEMPLATES: readonly DefaultPageTemplateDefinition[] = [
    {
        id: 'tpl-system-home',
        name: 'Home',
        description: 'Landing page with hero, featured APIs, and getting-started links.',
        contentType: 'BLOCK',
        kind: 'home',
        bodyStub: '# Welcome\n\nStart exploring our APIs and documentation.',
    },
    {
        id: 'tpl-system-catalog',
        name: 'Catalog',
        description: 'API catalog page listing available products and categories.',
        contentType: 'BLOCK',
        kind: 'catalog',
        bodyStub: '# API Catalog\n\nBrowse APIs available on this portal.',
    },
    {
        id: 'tpl-system-documentation',
        name: 'Documentation',
        description: 'General documentation page for guides and concepts.',
        contentType: 'BLOCK',
        kind: 'documentation',
        bodyStub: '# Documentation\n\nGuides, tutorials, and reference material.',
    },
    {
        id: 'tpl-system-api-reference',
        name: 'API Reference',
        description: 'OpenAPI-backed reference page for a single API.',
        contentType: 'OPENAPI',
        kind: 'api-reference',
        bodyStub: '<!-- OpenAPI specification will be rendered here -->',
    },
    {
        id: 'tpl-system-asyncapi',
        name: 'AsyncAPI',
        description: 'Event-driven API documentation from an AsyncAPI specification.',
        contentType: 'ASYNCAPI',
        kind: 'asyncapi',
        bodyStub: '<!-- AsyncAPI specification will be rendered here -->',
    },
    {
        id: 'tpl-system-html-blank',
        name: 'HTML blank',
        description: 'Empty HTML page for fully custom markup.',
        contentType: 'HTML',
        kind: 'html-blank',
        bodyStub: '<section>\n  <h1>Custom page</h1>\n  <p>Replace this content.</p>\n</section>',
    },
    {
        id: 'tpl-system-login',
        name: 'Login',
        description: 'Consumer login page layout stub.',
        contentType: 'BLOCK',
        kind: 'login',
        bodyStub: '# Sign in\n\nUse your credentials or a configured identity provider.',
    },
    {
        id: 'tpl-system-signup',
        name: 'Signup',
        description: 'Consumer registration page layout stub.',
        contentType: 'BLOCK',
        kind: 'signup',
        bodyStub: '# Create an account\n\nRegister to subscribe to APIs on this portal.',
    },
];

export function toSystemPageTemplate(definition: DefaultPageTemplateDefinition, now = Date.now()): PageTemplate {
    return {
        id: definition.id,
        name: definition.name,
        description: definition.description,
        contentType: definition.contentType,
        kind: definition.kind,
        system: true,
        bodyStub: definition.bodyStub,
        createdAt: now,
        updatedAt: now,
    };
}
