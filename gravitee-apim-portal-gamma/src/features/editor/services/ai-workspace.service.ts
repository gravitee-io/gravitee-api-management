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
import type { AiWorkspace, AiWorkspacesResponse } from '../entities/ai-workspace';

/** The workspace surfaced by the demo template and used as the default for AI blocks. */
export const DEMO_AI_WORKSPACE_ID = 'ai-workspace-enterprise';

/** A simpler demo workspace whose portal pages only use the composite group blocks. */
export const DEMO_AI_WORKSPACE_GROUPS_ID = 'ai-workspace-starter';

function buildUsageByDay(scale = 1): AiWorkspace['usageByDay'] {
    // 14 days of gently trending usage data for the usage-history chart.
    const base = [
        1800, 2400, 2100, 3200, 2800, 900, 700, 3600, 4200, 3900, 4800, 5200, 3100, 4550,
    ];
    const start = new Date('2026-07-08T00:00:00Z');
    return base.map((tokens, index) => {
        const scaled = Math.round(tokens * scale);
        const date = new Date(start);
        date.setUTCDate(start.getUTCDate() + index);
        const requests = Math.round(scaled / 210);
        const cost = Math.round((scaled / 1000) * 0.9 * 100) / 100;
        return {
            date: date.toISOString().slice(0, 10),
            tokens: scaled,
            requests,
            cost,
        };
    });
}

const MOCK_AI_WORKSPACES: AiWorkspace[] = [
    {
        id: DEMO_AI_WORKSPACE_ID,
        name: 'Enterprise AI',
        description:
            'A single, governed gateway to the models and AI tools your teams are cleared to use — one AI key, one budget, full observability.',
        status: 'active',
        providers: ['OpenAI', 'Anthropic', 'Google', 'Mistral', 'Meta'],
        aiKey: 'gv-ai-sk-demo1234abcd5678efgh9012ijkl3456mnop7f3d',
        keyCreatedAt: '2026-06-30',
        endpoint: 'https://ai.acme-corp.gravitee.io/v1',
        headerName: 'Authorization',
        budget: {
            used: 42.5,
            total: 100,
            currency: 'USD',
            resetInDays: 15,
            tokensUsed: 45210,
            requests: 1284,
        },
        models: [
            {
                id: 'gpt-4o',
                name: 'GPT-4o',
                provider: 'OpenAI',
                contextWindow: '128K',
                capabilities: ['chat', 'vision', 'tools'],
                tier: '$$$',
            },
            {
                id: 'claude-sonnet-4',
                name: 'Claude Sonnet 4',
                provider: 'Anthropic',
                contextWindow: '200K',
                capabilities: ['chat', 'vision', 'tools'],
                tier: '$$$',
            },
            {
                id: 'gemini-2-pro',
                name: 'Gemini 2 Pro',
                provider: 'Google',
                contextWindow: '1M',
                capabilities: ['chat', 'vision'],
                tier: '$$',
            },
            {
                id: 'mistral-large',
                name: 'Mistral Large',
                provider: 'Mistral',
                contextWindow: '128K',
                capabilities: ['chat', 'tools'],
                tier: '$$',
            },
            {
                id: 'llama-3-70b',
                name: 'Llama 3 70B',
                provider: 'Meta',
                contextWindow: '32K',
                capabilities: ['chat'],
                tier: '$',
            },
            {
                id: 'text-embedding-3',
                name: 'Embedding 3 Large',
                provider: 'OpenAI',
                contextWindow: '8K',
                capabilities: ['embeddings'],
                tier: '$',
            },
        ],
        usageByDay: buildUsageByDay(),
        usageByModel: [
            { model: 'GPT-4o', tokens: 21400, cost: 22.6 },
            { model: 'Claude Sonnet 4', tokens: 13800, cost: 14.1 },
            { model: 'Gemini 2 Pro', tokens: 6300, cost: 3.9 },
            { model: 'Mistral Large', tokens: 2600, cost: 1.4 },
            { model: 'Llama 3 70B', tokens: 1110, cost: 0.5 },
        ],
        includedApis: [
            {
                name: 'Payments API',
                description: 'Charge, refund, and reconcile transactions from within agent workflows.',
            },
            {
                name: 'User Management API',
                description: 'Look up and manage customer identities and entitlements.',
            },
            {
                name: 'Knowledge Base API',
                description: 'Retrieve indexed product docs for retrieval-augmented generation.',
            },
        ],
        mcp: {
            name: 'enterprise-ai-tools',
            url: 'https://ai.acme-corp.gravitee.io/mcp',
        },
    },
    {
        id: DEMO_AI_WORKSPACE_GROUPS_ID,
        name: 'Starter AI',
        description:
            'A lightweight workspace for trying the composite portal blocks — dashboard, credentials, and analytics.',
        status: 'active',
        providers: ['OpenAI', 'Anthropic'],
        aiKey: 'gv-ai-sk-starter98ab76cd54ef32gh10ij98kl76mn54op',
        keyCreatedAt: '2026-07-15',
        endpoint: 'https://ai.acme-corp.gravitee.io/starter/v1',
        headerName: 'Authorization',
        budget: {
            used: 18.2,
            total: 50,
            currency: 'USD',
            resetInDays: 22,
            tokensUsed: 18400,
            requests: 512,
        },
        models: [
            {
                id: 'gpt-4o-mini',
                name: 'GPT-4o mini',
                provider: 'OpenAI',
                contextWindow: '128K',
                capabilities: ['chat', 'tools'],
                tier: '$',
            },
            {
                id: 'claude-haiku-3-5',
                name: 'Claude 3.5 Haiku',
                provider: 'Anthropic',
                contextWindow: '200K',
                capabilities: ['chat', 'tools'],
                tier: '$',
            },
            {
                id: 'gpt-4o',
                name: 'GPT-4o',
                provider: 'OpenAI',
                contextWindow: '128K',
                capabilities: ['chat', 'vision', 'tools'],
                tier: '$$$',
            },
        ],
        usageByDay: buildUsageByDay(0.4),
        usageByModel: [
            { model: 'GPT-4o mini', tokens: 9200, cost: 4.1 },
            { model: 'Claude 3.5 Haiku', tokens: 6100, cost: 5.8 },
            { model: 'GPT-4o', tokens: 3100, cost: 8.3 },
        ],
        includedApis: [],
    },
];

export interface AiWorkspaceSearchParams {
    page?: number;
    size?: number;
    q?: string;
}

function filterAiWorkspaces({ q = '' }: AiWorkspaceSearchParams): AiWorkspace[] {
    const query = q.trim().toLowerCase();

    return MOCK_AI_WORKSPACES.filter(workspace => {
        if (!query) {
            return true;
        }

        return (
            workspace.name.toLowerCase().includes(query) ||
            workspace.description.toLowerCase().includes(query)
        );
    });
}

/**
 * Synchronous lookup used by AI blocks to render mock data without async plumbing.
 * Falls back to the demo workspace when the id is unknown or empty.
 */
export function getAiWorkspaceData(id?: string): AiWorkspace {
    const match = id ? MOCK_AI_WORKSPACES.find(workspace => workspace.id === id) : undefined;
    return match ?? MOCK_AI_WORKSPACES[0];
}

export async function getAiWorkspaceById(id: string): Promise<AiWorkspace | undefined> {
    return MOCK_AI_WORKSPACES.find(workspace => workspace.id === id);
}

export async function searchAiWorkspaces({
    page = 1,
    size = 20,
    q = '',
}: AiWorkspaceSearchParams = {}): Promise<AiWorkspacesResponse> {
    const filtered = filterAiWorkspaces({ q });
    const start = (page - 1) * size;
    const data = filtered.slice(start, start + size);

    return {
        data,
        metadata: {
            pagination: {
                current_page: page,
                size,
                total: filtered.length,
                total_pages: Math.max(1, Math.ceil(filtered.length / size)),
            },
        },
    };
}
