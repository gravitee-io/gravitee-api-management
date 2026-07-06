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
import type { NavGroup } from '@gravitee/graphene-core';
import { ArchiveIcon, RadioIcon, RocketIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import { observability } from './observability';
import { ROUTES } from './routes';

export function getNavGroups(): NavGroup[] {
    return [
        {
            label: 'General',
            items: [{ key: 'quick-start', title: ROUTES['quick-start'].label, icon: RocketIcon }],
        },
        {
            label: 'Manage',
            items: [
                { key: 'apis', title: ROUTES.apis.label, icon: RadioIcon },
                { key: 'api-products', title: ROUTES['api-products'].label, icon: ArchiveIcon },
            ],
        },
        // API Score lives under Observe alongside dashboards/logs/tracing (matches the product prototype).
        {
            ...observability.navGroup,
            items: [{ key: 'api-score', title: ROUTES['api-score'].label, icon: ShieldCheckIcon }, ...observability.navGroup.items],
        },
    ];
}
