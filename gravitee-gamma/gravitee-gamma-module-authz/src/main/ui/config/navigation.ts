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
import {
    BoxesIcon,
    BrainIcon,
    GlobeIcon,
    LayoutDashboardIcon,
    NetworkIcon,
    RefreshCwIcon,
    ShieldCheckIcon,
    SlidersHorizontalIcon,
    ZapIcon,
} from '@gravitee/graphene-core/icons';
import { ROUTES } from './routes';

export const NAV_GROUPS: NavGroup[] = [
    {
        label: 'Authorization',
        items: [{ key: 'dashboard', title: ROUTES.dashboard.label, icon: LayoutDashboardIcon }],
    },
    {
        label: 'Policy Management',
        items: [
            { key: 'mcps', title: ROUTES.mcps.label, icon: ShieldCheckIcon },
            { key: 'models', title: ROUTES.models.label, icon: BrainIcon },
            { key: 'apis', title: ROUTES.apis.label, icon: GlobeIcon },
            { key: 'custom-policies', title: ROUTES['custom-policies'].label, icon: SlidersHorizontalIcon },
        ],
    },
    {
        label: 'Policy Structure',
        items: [
            { key: 'entities', title: ROUTES.entities.label, icon: BoxesIcon },
            { key: 'actions', title: ROUTES.actions.label, icon: ZapIcon },
            { key: 'schema', title: ROUTES.schema.label, icon: NetworkIcon },
        ],
    },
    {
        label: 'Identity',
        items: [{ key: 'user-sync', title: ROUTES['user-sync'].label, icon: RefreshCwIcon }],
    },
];
