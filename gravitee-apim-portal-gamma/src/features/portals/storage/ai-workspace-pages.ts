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
import { getAiWorkspaceData } from '../../editor/services/ai-workspace.service';
import type { BlockNoteDocument } from '../types';

type Block = Record<string, unknown>;

function text(value: string): Record<string, unknown> {
    return { type: 'text', text: value, styles: {} };
}

function heading(level: number, value: string): Block {
    return { type: 'heading', props: { level }, content: [text(value)], children: [] };
}

function paragraph(value: string): Block {
    return { type: 'paragraph', content: value ? [text(value)] : [], children: [] };
}

function aiBlock(type: string, props: Record<string, unknown>): Block {
    return { type, props, content: [], children: [] };
}

function includedApisSection(workspaceId: string): Block[] {
    const workspace = getAiWorkspaceData(workspaceId);
    if (workspace.includedApis.length === 0) {
        return [];
    }
    return [
        heading(2, 'Also included in this workspace'),
        {
            type: 'graviteeSection',
            props: {
                title: '',
                subtitle: '',
                variant: 'plain',
                columns: '3',
                height: '0',
                contentWidth: 'auto',
                items: JSON.stringify(
                    workspace.includedApis.map(api => ({
                        icon: 'box',
                        title: api.name,
                        description: api.description,
                    })),
                ),
            },
            content: [],
            children: [],
        },
    ];
}

export interface AiWorkspacePageDefinition {
    readonly title: string;
    readonly document: BlockNoteDocument;
}

/** Page 1 — Getting Started: credentials kit to make the first call. */
export function buildGettingStartedDocument(workspaceId: string): BlockNoteDocument {
    return [
        aiBlock('graviteeAiWorkspaceOverview', { workspaceId, showProviders: 'true', showStatus: 'true' }),
        heading(2, 'Get started'),
        paragraph('Copy your key and endpoint, then run a snippet — that is all you need for your first call.'),
        aiBlock('graviteeAiCredentials', { workspaceId, languages: 'curl,python', useRealKey: 'false' }),
        heading(2, 'Try it out'),
        paragraph('Send a prompt through the workspace gateway and inspect the matching request and response payloads.'),
        aiBlock('graviteeAiTryIt', { workspaceId }),
    ] as BlockNoteDocument;
}

/** Page 2 — Overview dashboard: models + brief budget + brief usage. */
export function buildAvailableModelsDocument(workspaceId: string): BlockNoteDocument {
    return [
        heading(1, 'Overview'),
        paragraph('A snapshot of the models you can call and how much of your budget you have used.'),
        aiBlock('graviteeAiDashboard', { workspaceId }),
        ...includedApisSection(workspaceId),
    ] as BlockNoteDocument;
}

/** Page 3 — Code Snippets (with optional MCP setup). */
export function buildCodeSnippetsDocument(workspaceId: string): BlockNoteDocument {
    const workspace = getAiWorkspaceData(workspaceId);
    const blocks: Block[] = [
        heading(1, 'Code snippets'),
        paragraph('Ready-to-run examples pre-filled with your workspace endpoint. Swap in your AI key to go live.'),
        aiBlock('graviteeAiSnippets', { workspaceId, languages: '', useRealKey: 'false' }),
    ];

    if (workspace.mcp) {
        blocks.push(
            heading(2, 'MCP setup'),
            paragraph('Add the workspace MCP server to your AI client (Cursor, VS Code, Claude Desktop).'),
            {
                type: 'graviteeInstallMcp',
                props: {
                    name: workspace.mcp.name,
                    transport: 'http',
                    url: workspace.mcp.url,
                    headers: JSON.stringify({ Authorization: 'Bearer YOUR_AI_KEY' }),
                    command: '',
                    args: '',
                    env: '',
                    clients: 'cursor, vscode, claude-desktop',
                },
                content: [],
                children: [],
            },
        );
    }

    return blocks as BlockNoteDocument;
}

/** Page 4 — My Usage: detailed budget + usage over time. */
export function buildMyUsageDocument(workspaceId: string): BlockNoteDocument {
    return [
        heading(1, 'My usage'),
        paragraph('Track your consumption and remaining budget in real time.'),
        aiBlock('graviteeAiAnalytics', { workspaceId, range: '14' }),
    ] as BlockNoteDocument;
}

/** Group-only pages — each page is a single composite block. */
export function buildGroupDashboardDocument(workspaceId: string): BlockNoteDocument {
    return [aiBlock('graviteeAiDashboard', { workspaceId })] as BlockNoteDocument;
}

export function buildGroupCredentialsDocument(workspaceId: string): BlockNoteDocument {
    return [
        aiBlock('graviteeAiCredentials', { workspaceId, languages: '', useRealKey: 'false' }),
        aiBlock('graviteeAiTryIt', { workspaceId }),
    ] as BlockNoteDocument;
}

export function buildGroupAnalyticsDocument(workspaceId: string): BlockNoteDocument {
    return [aiBlock('graviteeAiAnalytics', { workspaceId, range: '14' })] as BlockNoteDocument;
}

/**
 * The ordered set of auto-generated pages for an AI workspace nav item.
 * The MCP page is folded into "Code Snippets"; when the workspace has no MCP
 * asset the snippets page simply omits the MCP block.
 */
export function buildAiWorkspacePageDefinitions(workspaceId: string): AiWorkspacePageDefinition[] {
    return [
        { title: 'Getting Started', document: buildGettingStartedDocument(workspaceId) },
        { title: 'Overview', document: buildAvailableModelsDocument(workspaceId) },
        { title: 'Code Snippets', document: buildCodeSnippetsDocument(workspaceId) },
        { title: 'My Usage', document: buildMyUsageDocument(workspaceId) },
    ];
}

/** Minimal 3-page workspace that only uses the composite group blocks. */
export function buildAiWorkspaceGroupPageDefinitions(workspaceId: string): AiWorkspacePageDefinition[] {
    return [
        { title: 'Dashboard', document: buildGroupDashboardDocument(workspaceId) },
        { title: 'Credentials', document: buildGroupCredentialsDocument(workspaceId) },
        { title: 'Analytics', document: buildGroupAnalyticsDocument(workspaceId) },
    ];
}
