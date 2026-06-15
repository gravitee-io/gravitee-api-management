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
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { MODULE_ICONS } from '../../../../features/modules';
import type { Accent } from '../accents';

/** Plugin ids of modules we render a card for. Must match `plugin.properties#id` of
 *  the corresponding Gamma module (`gravitee-gamma-module-<id>`). */
export type ModuleId = 'aim' | 'apim' | 'platform' | 'authz' | 'esm';

export interface Application {
    readonly title: string;
    readonly description: string;
    readonly moduleId: ModuleId;
    readonly Icon: LucideIcon;
    readonly accent: Accent;
    readonly emptyState: {
        readonly cta: string;
        readonly ctaPath: string;
    };
}

/**
 * Static catalog of application cards. Each entry references the backend plugin id
 * directly — same source the app switcher consumes from `GET /organizations/{orgId}/modules`.
 * Cards whose `moduleId` is absent from that response render without a link or "Open →" CTA.
 */
export const APPLICATIONS: readonly Application[] = [
    {
        title: 'Agent Management',
        description: 'Discover, secure, build, and observe AI agents, MCP servers, and LLM integrations.',
        moduleId: 'aim',
        Icon: MODULE_ICONS['aim'],
        accent: 'highlight',
        emptyState: { cta: 'Add Integration', ctaPath: 'import/models' },
    },
    {
        title: 'API Management',
        description: 'Design, deploy, and manage your HTTP APIs with full lifecycle governance.',
        moduleId: 'apim',
        Icon: MODULE_ICONS['apim'],
        accent: 'primary',
        emptyState: { cta: 'Create your first API', ctaPath: 'apis/new' },
    },
    {
        title: 'Platform Management',
        description: 'Manage applications, subscribe to APIs, and monitor consumption from a single dashboard.',
        moduleId: 'platform',
        Icon: MODULE_ICONS['platform'],
        accent: 'accent',
        emptyState: { cta: 'Register an application', ctaPath: 'applications/new' },
    },
    {
        title: 'Authorization Management',
        description: 'Define fine-grained authorization rules, relationship tuples, and scopes across the platform.',
        moduleId: 'authz',
        Icon: MODULE_ICONS['authz'],
        accent: 'success',
        emptyState: { cta: 'Create your first policy', ctaPath: 'policies/new' },
    },
    {
        title: 'Event Stream Management',
        description: 'Register Kafka clusters, expose governed Kafka services, and federate them into an event mesh.',
        moduleId: 'esm',
        Icon: MODULE_ICONS['esm'],
        accent: 'muted',
        emptyState: { cta: 'Register a cluster', ctaPath: 'clusters' },
    },
];

export function buildModulePath(envHrid: string, moduleId: ModuleId): string {
    return `/environments/${envHrid}/${moduleId}`;
}
