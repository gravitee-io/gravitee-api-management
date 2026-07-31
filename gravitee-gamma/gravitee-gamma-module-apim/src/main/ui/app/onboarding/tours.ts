/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ActivityIcon, ArchiveIcon, RadioIcon, RocketIcon, SparklesIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import type { RouteKey } from '../../config/routes';

export interface CoachmarkStep {
    readonly id: string;
    readonly icon: LucideIcon;
    readonly title: string;
    readonly description: string;
    readonly navKey?: RouteKey;
}

export interface CoachmarkTourDefinition {
    readonly id: string;
    readonly steps: readonly CoachmarkStep[];
}

export const APIM_OVERVIEW_TOUR_ID = 'apim-overview';

export const APIM_OVERVIEW_TOUR: CoachmarkTourDefinition = {
    id: APIM_OVERVIEW_TOUR_ID,
    steps: [
        {
            id: 'welcome',
            icon: SparklesIcon,
            title: 'Welcome to API Management',
            description:
                'This is your command center for HTTP APIs. Use the sidebar to move between building proxies and products, and observing traffic. This quick tour walks you through each section.',
            navKey: 'quick-start',
        },
        {
            id: 'api-proxies',
            icon: RadioIcon,
            title: 'Start with API Proxies',
            description:
                'This is the API Proxies section. A proxy secures and observes an upstream service. Import an OpenAPI spec or build from scratch, then add authentication, rate limiting, and traffic policies in front of any HTTP, GraphQL, gRPC, or WebSocket backend.',
            navKey: 'apis',
        },
        {
            id: 'api-products',
            icon: ArchiveIcon,
            title: 'Bundle them into API Products',
            description:
                'This is the API Products section. A product groups one or more proxies into a single consumable surface. Define shared plans on the product so consumers subscribe once to access the whole bundle through the developer portal.',
            navKey: 'api-products',
        },
        {
            id: 'observe',
            icon: ActivityIcon,
            title: 'Observe your traffic',
            description:
                'This is the Observability section. Once an API is deployed, built-in dashboards, request logs, and distributed traces let you track health and usage so you can spot issues before your consumers do.',
            navKey: 'observe/dashboards',
        },
        {
            id: 'next',
            icon: RocketIcon,
            title: "You're ready to go",
            description:
                'Create your first API Proxy from the Quick Start page whenever you are ready. You can replay this tour any time from the "Take the tour" button on the Quick Start page.',
            navKey: 'quick-start',
        },
    ],
};

export const TOURS: Record<string, CoachmarkTourDefinition> = {
    [APIM_OVERVIEW_TOUR_ID]: APIM_OVERVIEW_TOUR,
};
