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

/** A single AI model exposed through an AI workspace. */
export interface AiModel {
    readonly id: string;
    readonly name: string;
    readonly provider: string;
    /** Human-friendly context window, e.g. "128K". */
    readonly contextWindow: string;
    /** Capabilities such as "chat", "vision", "embeddings", "tools". */
    readonly capabilities: string[];
    /** Relative price indicator, e.g. "$", "$$", "$$$". */
    readonly tier: string;
}

/** Per-day usage sample for the usage history block. */
export interface AiUsageDailyEntry {
    readonly date: string;
    readonly tokens: number;
    readonly requests: number;
    readonly cost: number;
}

/** Per-model usage breakdown. */
export interface AiUsageModelEntry {
    readonly model: string;
    readonly tokens: number;
    readonly cost: number;
}

export interface AiWorkspaceBudget {
    readonly used: number;
    readonly total: number;
    readonly currency: string;
    readonly resetInDays: number;
    readonly tokensUsed: number;
    readonly requests: number;
}

/** An included API / API Product mentioned (but not fully navigated) in a workspace. */
export interface AiWorkspaceIncludedApi {
    readonly name: string;
    readonly description: string;
    readonly docsUrl?: string;
}

export interface AiWorkspaceMcpAsset {
    readonly name: string;
    readonly url: string;
}

export interface AiWorkspace {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly status: 'active' | 'suspended';
    readonly providers: string[];
    /** The Gravitee-issued AI key (an API key under the hood). */
    readonly aiKey: string;
    readonly keyCreatedAt: string;
    /** OpenAI-compatible gateway base URL. */
    readonly endpoint: string;
    /** Header used to pass the AI key, e.g. "Authorization" or "X-Gravitee-AI-Key". */
    readonly headerName: string;
    readonly budget: AiWorkspaceBudget;
    readonly models: AiModel[];
    readonly usageByDay: AiUsageDailyEntry[];
    readonly usageByModel: AiUsageModelEntry[];
    readonly includedApis: AiWorkspaceIncludedApi[];
    readonly mcp?: AiWorkspaceMcpAsset;
}

export interface AiWorkspacesResponse {
    data?: AiWorkspace[];
    metadata?: {
        pagination?: {
            current_page?: number;
            size?: number;
            total?: number;
            total_pages?: number;
        };
    };
}
