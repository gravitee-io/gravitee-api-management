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
import { DEMO_AI_WORKSPACE_GROUPS_ID, DEMO_AI_WORKSPACE_ID } from '../../editor/services/ai-workspace.service';
import type { BlockNoteDocument, PageContent, PortalNavigationItem } from '../types';
import {
    buildAvailableModelsDocument,
    buildCodeSnippetsDocument,
    buildGettingStartedDocument,
    buildGroupAnalyticsDocument,
    buildGroupCredentialsDocument,
    buildGroupDashboardDocument,
    buildMyUsageDocument,
} from './ai-workspace-pages';
import { saveNavItem } from './navigation-items.storage';
import { savePageContent } from './page-contents.storage';

type Block = Record<string, unknown>;

function text(value: string, styles: Record<string, boolean> = {}): Record<string, unknown> {
    return { type: 'text', text: value, styles };
}

function heading(level: number, value: string): Block {
    return { type: 'heading', props: { level }, content: [text(value)], children: [] };
}

function paragraph(value: string): Block {
    return { type: 'paragraph', content: value ? [text(value)] : [], children: [] };
}

function bulletItem(value: string): Block {
    return { type: 'bulletListItem', content: [text(value)], children: [] };
}

function banner(title: string, subtitle: string): Block {
    return {
        type: 'graviteeBanner',
        props: {
            title,
            subtitle,
            variant: 'gradient',
            buttons: JSON.stringify([{ label: 'Open your workspace', link: '/getting-started-aiw002', variant: 'primary' }]),
            backgroundImage: '',
            height: '0',
        },
        content: [],
        children: [],
    };
}

function section(title: string, subtitle: string, items: Array<{ icon: string; title: string; description: string }>): Block {
    return {
        type: 'graviteeSection',
        props: {
            title,
            subtitle,
            variant: 'plain',
            columns: '3',
            items: JSON.stringify(items),
            height: '0',
            contentWidth: 'auto',
        },
        content: [],
        children: [],
    };
}

// ---------------------------------------------------------------------------
// Static (non-workspace) page documents
// ---------------------------------------------------------------------------

function homeDocument(): BlockNoteDocument {
    return [
        banner(
            'Enterprise AI Developer Portal',
            'Ship AI features faster with one governed gateway to every model your team is cleared to use.',
        ),
        section('Everything you need, in one place', '', [
            {
                icon: 'key',
                title: 'One AI key',
                description: 'A single Gravitee-issued key unlocks every model and tool in your workspace.',
            },
            {
                icon: 'gauge',
                title: 'Built-in budgets',
                description: 'Spend is capped and monitored automatically — no surprise bills.',
            },
            {
                icon: 'shield',
                title: 'Governed & observable',
                description: 'All traffic flows through the Gravitee gateway with full visibility.',
            },
        ]),
        heading(2, 'Get started in minutes'),
        paragraph('Head to your AI Workspace to grab your key, explore the available models, and copy a working code snippet.'),
    ] as BlockNoteDocument;
}

function authenticationDocument(): BlockNoteDocument {
    return [
        heading(1, 'Authentication'),
        paragraph('Every request to your AI workspace is authenticated with your AI key — a Gravitee-issued API key scoped to your workspace and budget.'),
        heading(2, 'How it works'),
        bulletItem('Pass your AI key as a Bearer token in the Authorization header.'),
        bulletItem('The Gravitee gateway validates the key, enforces your budget, and routes to the right provider.'),
        bulletItem('Usage is metered per request so you always know where your budget goes.'),
        heading(2, 'Rate limits'),
        paragraph('Requests are subject to per-workspace rate limits. If you exceed them, the gateway returns HTTP 429 — back off and retry with exponential delay.'),
    ] as BlockNoteDocument;
}

function bestPracticesDocument(): BlockNoteDocument {
    return [
        heading(1, 'Best practices'),
        heading(2, 'Cost optimization'),
        bulletItem('Pick the smallest model that meets your quality bar — reserve premium models for hard tasks.'),
        bulletItem('Cache responses for repeated prompts and set sensible max_tokens limits.'),
        bulletItem('Stream responses to improve perceived latency for end users.'),
        heading(2, 'Prompt engineering'),
        bulletItem('Give the model a clear role and explicit, structured instructions.'),
        bulletItem('Provide examples for the format you expect back.'),
        bulletItem('Keep system prompts concise; move variable context into user messages.'),
    ] as BlockNoteDocument;
}

function changelogDocument(): BlockNoteDocument {
    return [
        heading(1, 'Changelog'),
        heading(2, 'July 2026'),
        bulletItem('Added Gemini 2 Pro and Mistral Large to the Enterprise AI workspace.'),
        bulletItem('Introduced per-model spend breakdown in the usage dashboard.'),
        heading(2, 'June 2026'),
        bulletItem('Launched the Enterprise AI workspace with GPT-4o and Claude Sonnet 4.'),
        bulletItem('Enabled MCP tool access through the Gravitee gateway.'),
    ] as BlockNoteDocument;
}

// ---------------------------------------------------------------------------
// Navigation + content
// ---------------------------------------------------------------------------

function navId(portalId: string, suffix: string): string {
    return `${portalId}-${suffix}`;
}

function createAiWorkspaceNavigation(portalId: string): PortalNavigationItem[] {
    const workspace = navId(portalId, 'nav-workspace');
    const starterWorkspace = navId(portalId, 'nav-starter-workspace');
    const documentation = navId(portalId, 'fld-documentation');

    return [
        { id: navId(portalId, 'nav-home'), portalId, title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-aiw001', published: true },

        {
            id: workspace,
            portalId,
            title: 'Enterprise AI',
            type: 'AI_WORKSPACE',
            aiWorkspaceId: DEMO_AI_WORKSPACE_ID,
            parentId: null,
            order: 1,
            slug: 'enterprise-ai-aiw100',
            published: true,
        },
        { id: navId(portalId, 'nav-getting-started'), portalId, title: 'Getting Started', type: 'PAGE', parentId: workspace, order: 0, slug: 'getting-started-aiw002', published: true },
        { id: navId(portalId, 'nav-available-models'), portalId, title: 'Overview', type: 'PAGE', parentId: workspace, order: 1, slug: 'available-models-aiw003', published: true },
        { id: navId(portalId, 'nav-code-snippets'), portalId, title: 'Code Snippets', type: 'PAGE', parentId: workspace, order: 2, slug: 'code-snippets-aiw004', published: true },
        { id: navId(portalId, 'nav-my-usage'), portalId, title: 'My Usage', type: 'PAGE', parentId: workspace, order: 3, slug: 'my-usage-aiw005', published: true },

        {
            id: starterWorkspace,
            portalId,
            title: 'Starter AI',
            type: 'AI_WORKSPACE',
            aiWorkspaceId: DEMO_AI_WORKSPACE_GROUPS_ID,
            parentId: null,
            order: 2,
            slug: 'starter-ai-aiw110',
            published: true,
        },
        { id: navId(portalId, 'nav-starter-dashboard'), portalId, title: 'Dashboard', type: 'PAGE', parentId: starterWorkspace, order: 0, slug: 'starter-dashboard-aiw111', published: true },
        { id: navId(portalId, 'nav-starter-credentials'), portalId, title: 'Credentials', type: 'PAGE', parentId: starterWorkspace, order: 1, slug: 'starter-credentials-aiw112', published: true },
        { id: navId(portalId, 'nav-starter-analytics'), portalId, title: 'Analytics', type: 'PAGE', parentId: starterWorkspace, order: 2, slug: 'starter-analytics-aiw113', published: true },

        { id: documentation, portalId, title: 'Documentation', type: 'FOLDER', parentId: null, order: 3, slug: 'documentation-aiw101', published: true },
        { id: navId(portalId, 'nav-authentication'), portalId, title: 'Authentication', type: 'PAGE', parentId: documentation, order: 0, slug: 'authentication-aiw006', published: true },
        { id: navId(portalId, 'nav-best-practices'), portalId, title: 'Best Practices', type: 'PAGE', parentId: documentation, order: 1, slug: 'best-practices-aiw007', published: true },
        { id: navId(portalId, 'nav-changelog'), portalId, title: 'Changelog', type: 'PAGE', parentId: documentation, order: 2, slug: 'changelog-aiw008', published: true },

        {
            id: navId(portalId, 'footer-status'), portalId, title: 'Gateway Status', type: 'LINK',
            parentId: null, order: 0, slug: 'status-footer-aiw201', url: 'https://status.acme-corp.gravitee.io', area: 'FOOTER', published: true,
        },
        {
            id: navId(portalId, 'menu-logout'), portalId, title: 'Log out', type: 'LINK',
            parentId: null, order: 0, slug: 'logout-menu-aiw301', url: '/logout', area: 'USER_MENU', published: true,
        },
    ];
}

function createAiWorkspacePageContents(portalId: string, navItems: readonly PortalNavigationItem[]): PageContent[] {
    const documentMap: Record<string, () => BlockNoteDocument> = {
        'nav-home': homeDocument,
        'nav-getting-started': () => buildGettingStartedDocument(DEMO_AI_WORKSPACE_ID),
        'nav-available-models': () => buildAvailableModelsDocument(DEMO_AI_WORKSPACE_ID),
        'nav-code-snippets': () => buildCodeSnippetsDocument(DEMO_AI_WORKSPACE_ID),
        'nav-my-usage': () => buildMyUsageDocument(DEMO_AI_WORKSPACE_ID),
        'nav-starter-dashboard': () => buildGroupDashboardDocument(DEMO_AI_WORKSPACE_GROUPS_ID),
        'nav-starter-credentials': () => buildGroupCredentialsDocument(DEMO_AI_WORKSPACE_GROUPS_ID),
        'nav-starter-analytics': () => buildGroupAnalyticsDocument(DEMO_AI_WORKSPACE_GROUPS_ID),
        'nav-authentication': authenticationDocument,
        'nav-best-practices': bestPracticesDocument,
        'nav-changelog': changelogDocument,
    };

    return navItems
        .filter((item): item is PortalNavigationItem & { type: 'PAGE' } => item.type === 'PAGE')
        .map(item => {
            const key = item.id.slice(`${portalId}-`.length);
            const docFn = documentMap[key];
            if (!docFn) {
                throw new Error(`Missing AI Workspace page document for navigation key: ${key}`);
            }
            return {
                id: `page-content-${item.id}`,
                portalId,
                navigationItemId: item.id,
                contentType: 'BLOCK' as const,
                document: docFn(),
            };
        });
}

export async function seedAiWorkspacePortal(portalId: string): Promise<void> {
    const navItems = createAiWorkspaceNavigation(portalId);
    const pageContents = createAiWorkspacePageContents(portalId, navItems);

    await Promise.all(navItems.map(item => saveNavItem(item)));
    await Promise.all(pageContents.map(content => savePageContent(content)));
}
