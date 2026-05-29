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
import { PlugZapIcon, PuzzleIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import type { Accent } from '../accents';
import type { ModuleId } from '../application-card/applications';

export interface GetStartedStep {
    readonly title: string;
    readonly description: string;
    readonly Icon: LucideIcon;
    readonly accent: Accent;
    readonly moduleId: ModuleId;
    readonly subPath: string;
}

export const GET_STARTED_STEPS: readonly GetStartedStep[] = [
    {
        title: 'Add Integration',
        description: 'Connect to any platform like OpenAI or AWS Bedrock to import models, tools and agents.',
        Icon: PlugZapIcon,
        accent: 'success',
        moduleId: 'aim',
        subPath: 'import/models',
    },
    {
        title: 'Create MCP Proxy',
        description: 'Create a MCP proxy to expose and govern your tool servers.',
        Icon: PuzzleIcon,
        accent: 'highlight',
        moduleId: 'aim',
        subPath: 'mcp-proxy/new',
    },
    {
        title: 'Protect with FGA',
        description: 'Apply fine-grained authorization policies to control who and what can access your agents and tools.',
        Icon: ShieldCheckIcon,
        accent: 'primary',
        moduleId: 'authz',
        subPath: 'apis',
    },
];
