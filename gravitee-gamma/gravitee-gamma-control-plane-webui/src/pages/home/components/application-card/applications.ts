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
export type ModuleId = 'aim' | 'apim' | 'platform' | 'authz' | 'esm' | 'edge';

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
    /**
     * Upsell content shown in the upgrade dialog. Present only for license-gated modules.
     * Core modules that always ship with the installation (e.g. API Management, Platform) omit it
     * and are therefore never rendered in the locked state.
     */
    readonly upgrade?: {
        readonly features: readonly string[];
    };
}

/**
 * Destination of the "Request an enterprise license" CTA in the upgrade dialog.
 * Aligned with the classic console (`gio-license-data` / `bootstrapApplication`), which links to
 * the self-hosted trial page. Gamma can also run self-hosted with a restricted license.
 */
export const REQUEST_ENTERPRISE_LICENSE_URL = 'https://gravitee.io/self-hosted-trial';

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
        emptyState: { cta: 'Add Integration', ctaPath: '' },
        upgrade: {
            features: [
                'Catalog and secure LLM, MCP, and A2A proxies',
                'Govern agent access with fine-grained policies',
                'Observe agent traffic and usage in real time',
            ],
        },
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
        upgrade: {
            features: [
                'Model relationship-based access control (ReBAC)',
                'Define fine-grained policies, tuples, and scopes',
                'Enforce authorization decisions at the edge',
            ],
        },
    },
    {
        title: 'Event Stream Management',
        description: 'Register Kafka clusters, expose governed Kafka services, and federate them into an event mesh.',
        moduleId: 'esm',
        Icon: MODULE_ICONS['esm'],
        accent: 'muted',
        emptyState: { cta: 'Register a cluster', ctaPath: 'clusters' },
        upgrade: {
            features: [
                'Register and sync Kafka clusters in minutes',
                'Expose streams as governed APIs for external consumers',
                'Observe real-time data flows across topics and agents',
            ],
        },
    },
    {
        title: 'Edge Management',
        description: 'Monitor and manage your fleet of Edge Daemon agents.',
        moduleId: 'edge',
        Icon: MODULE_ICONS['edge'],
        accent: 'highlight',
        emptyState: { cta: 'Open Edge Management', ctaPath: '' },
        upgrade: {
            features: [
                'Configure proxy, DNS, and Shadow AI controls',
                'Monitor devices and proxied traffic in real time',
                'Govern AI usage across your edge fleet',
            ],
        },
    },
];

export function buildModulePath(envHrid: string, moduleId: ModuleId): string {
    return `/environments/${envHrid}/${moduleId}`;
}
