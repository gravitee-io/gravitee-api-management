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
import { BotIcon, KeyIcon, LayoutDashboardIcon, RadioIcon, ScrollTextIcon, WaypointsIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import type { Accent } from '../accents';

/** Plugin ids of modules we render a card for. Must match `plugin.properties#id` of
 *  the corresponding Gamma module (`gravitee-gamma-module-<id>`). Typed as a literal
 *  union so the badge map (`DynamicBadges`) can key on the same names — review #13. */
export type ModuleId = 'aim' | 'apim' | 'platform' | 'catalog' | 'authz';

export type Application =
    | {
          readonly kind: 'module';
          readonly title: string;
          readonly description: string;
          /**
           * Optional summary computed from a live API call (e.g. "24 APIs"). When undefined,
           * the badge slot is omitted entirely — no mock numbers, no placeholder.
           */
          readonly badge?: string;
          readonly moduleId: ModuleId;
          readonly Icon: LucideIcon;
          readonly accent: Accent;
      }
    | {
          readonly kind: 'coming-soon';
          readonly title: string;
          readonly description: string;
          readonly Icon: LucideIcon;
          readonly accent: Accent;
      };

export const COMING_SOON_BADGE = 'Coming soon';

/**
 * Static catalog of application cards. Module cards reference the backend plugin id
 * directly — same source the app switcher consumes from `GET /organizations/{orgId}/modules`.
 * Cards whose `moduleId` is absent from that response are not rendered (matching the
 * app switcher behavior: missing license / missing deployment / unknown id all collapse
 * to "not shown"). Counts (`badge`) come from live hooks at render time.
 *
 * "Coming soon" cards are always rendered — they advertise upcoming products that have
 * no backend representation yet.
 */
export const APPLICATIONS: readonly Application[] = [
    {
        kind: 'module',
        title: 'Agent Management',
        description: 'Discover, secure, build, and observe AI agents, MCP servers, and LLM integrations.',
        moduleId: 'aim', // plugin.properties#id of gravitee-gamma-module-aim
        Icon: BotIcon,
        accent: 'highlight', // AI/agentic — pairs with "Build an Agent"
    },
    {
        kind: 'module',
        title: 'API Management',
        description: 'Design, deploy, and manage your HTTP APIs with full lifecycle governance.',
        moduleId: 'apim', // plugin.properties#id of gravitee-gamma-module-apim
        Icon: RadioIcon,
        accent: 'primary', // brand core — pairs with "Expose APIs to Agents"
    },
    {
        kind: 'module',
        title: 'Platform',
        description: 'Manage applications, subscribe to APIs, and monitor consumption from a single dashboard.',
        moduleId: 'platform', // plugin.properties#id of gravitee-gamma-module-platform
        Icon: LayoutDashboardIcon,
        accent: 'accent', // neutral — operational management surface
    },
    {
        kind: 'module',
        title: 'Catalog',
        description: 'Browse available APIs, event streams, and MCP tools in a unified, searchable catalog.',
        moduleId: 'catalog',
        Icon: ScrollTextIcon,
        accent: 'accent', // neutral — inventory/listing
    },
    {
        kind: 'module',
        title: 'Authorization',
        description: 'Define fine-grained authorization rules, relationship tuples, and scopes across the platform.',
        moduleId: 'authz', // plugin.properties#id of gravitee-gamma-module-authz
        Icon: KeyIcon,
        accent: 'success', // allows / grants
    },
    {
        kind: 'coming-soon',
        title: 'Event API Management',
        description: 'Manage event-driven architectures with Kafka clusters, topics, and streaming protocols.',
        Icon: WaypointsIcon,
        accent: 'muted', // neutral (already opacity-60)
    },
];

export function buildModulePath(envHrid: string, moduleId: ModuleId): string {
    return `/environments/${envHrid}/${moduleId}`;
}
