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

import { MODULE_CATALOG, MODULE_ICONS, type ModuleId } from '../../../../features/modules';
import type { Accent } from '../accents';

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

type CardContent = Omit<Application, 'title' | 'moduleId' | 'Icon'>;

/** Keyed by module id so that adding a product to the catalog fails to compile until it has a card. */
const CARD_CONTENT: Record<ModuleId, CardContent> = {
    apim: {
        description: 'Design, deploy, and manage your HTTP APIs with full lifecycle governance.',
        accent: 'primary',
        emptyState: { cta: 'Create your first API', ctaPath: 'apis/new' },
    },
    aim: {
        description: 'Discover, secure, build, and observe AI agents, MCP servers, and LLM integrations.',
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
    authz: {
        description: 'Define fine-grained authorization rules, relationship tuples, and scopes across the platform.',
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
    act: {
        description: 'Build the agents that guard your platform, give them the tools and sandboxes they need, and watch what they do.',
        accent: 'highlight',
        emptyState: { cta: 'Open Guardian Agent', ctaPath: '' },
        upgrade: {
            features: [
                'Build agents that act as guardrails for your other agents',
                'Give guardian agents the tools and sandboxes they need',
                'Watch every action your guardian agents take',
            ],
        },
    },
    esm: {
        description: 'Register Kafka clusters, expose governed Kafka services, and federate them into an event mesh.',
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
    portals: {
        description: 'Design and manage developer portal experiences.',
        accent: 'primary',
        emptyState: { cta: 'Open Developer Portals', ctaPath: '' },
    },
    platform: {
        description: 'Manage applications, subscribe to APIs, and monitor consumption from a single dashboard.',
        accent: 'accent',
        emptyState: { cta: 'Register an application', ctaPath: 'applications/new' },
    },
    edge: {
        description: 'Monitor and manage your fleet of Edge Daemon agents.',
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
};

/**
 * One card per visible catalog product, in catalog order — the same order as the app switcher.
 * A license-gated card whose `moduleId` is absent from `GET /organizations/{orgId}/modules` renders locked.
 */
export const APPLICATIONS: readonly Application[] = MODULE_CATALOG.filter(product => !('hidden' in product)).map(product => ({
    title: product.label,
    moduleId: product.id,
    Icon: MODULE_ICONS[product.id],
    ...CARD_CONTENT[product.id],
}));

export function buildModulePath(envHrid: string, moduleId: ModuleId): string {
    return `/environments/${envHrid}/${moduleId}`;
}
