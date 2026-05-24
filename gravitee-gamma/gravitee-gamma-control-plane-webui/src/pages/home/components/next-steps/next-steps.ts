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
import { BotIcon, EyeIcon, NetworkIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import type { Accent } from '../accents';

export interface NextStep {
    readonly title: string;
    readonly description: string;
    readonly Icon: LucideIcon;
    readonly accent: Accent;
}

/**
 * Suggested next steps are advisory hints, not navigation entry points — they don't deep-link
 * to a specific module page (the mapping from "step" to module is rarely 1:1, and clicking on
 * "Review Observability" landing on the APIM list would feel arbitrary). Rendered as static
 * informational tiles inside the panel.
 *
 * All steps share a neutral `muted` accent: the surrounding banner already carries the
 * `highlight` colour wash, so per-step accents would compete with it. Keeping the step
 * icons monochrome matches the mockup and lets the panel read as one cohesive block.
 */
export const NEXT_STEPS: readonly NextStep[] = [
    {
        title: 'Expose APIs to Agents',
        description: 'Turn HTTP APIs into MCP tools for AI agents',
        Icon: NetworkIcon,
        accent: 'muted',
    },
    {
        title: 'Build an Agent',
        description: 'Assemble an agent from your registered MCP servers',
        Icon: BotIcon,
        accent: 'muted',
    },
    {
        title: 'Review Observability',
        description: 'Check logs, dashboards, and lineage across services',
        Icon: EyeIcon,
        accent: 'muted',
    },
];
