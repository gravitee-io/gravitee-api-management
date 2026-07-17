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

export interface GraviteeSlotComponent {
    readonly id: string;
    readonly label: string;
    readonly description: string;
}

export const GRAVITEE_SLOT_COMPONENTS: readonly GraviteeSlotComponent[] = [
    {
        id: 'api-catalog',
        label: 'API Catalog',
        description: 'Published APIs with customizable tile layout',
    },
    {
        id: 'subscription-viewer',
        label: 'Subscription Viewer',
        description: 'Table of subscriptions with details panel',
    },
    {
        id: 'subscription-flow',
        label: 'Subscription Flow',
        description: 'Multi-step wizard to subscribe to the current API',
    },
    {
        id: 'applications',
        label: 'Applications',
        description: 'Manage applications, settings, and members',
    },
] as const;

export function buildSlotSnippet(componentId: string): string {
    return `<div data-gravitee-component="${componentId}"></div>`;
}
