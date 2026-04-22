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

import { BarChart3, FileText, LayoutDashboard, Network, Package, PencilRuler, Radio, Wrench } from 'lucide-react';

import { ROUTES } from './routes';

export const NAV_GROUPS: NavGroup[] = [
    {
        label: 'Overview',
        items: [{ key: 'dashboard', title: ROUTES.dashboard.label, icon: LayoutDashboard }],
    },
    {
        label: 'Manage',
        items: [
            { key: 'apis', title: ROUTES.apis.label, icon: Radio },
            { key: 'api-products', title: ROUTES['api-products'].label, icon: Package },
        ],
    },
    {
        label: 'Design',
        items: [{ key: 'api-designer', title: ROUTES['api-designer'].label, icon: PencilRuler }],
    },
    {
        label: 'Observe',
        items: [
            { key: 'dashboards', title: ROUTES.dashboards.label, icon: BarChart3 },
            { key: 'logs', title: ROUTES.logs.label, icon: FileText },
            { key: 'lineage', title: ROUTES.lineage.label, icon: Network },
        ],
    },
    {
        label: '',
        items: [{ key: 'migration-studio', title: ROUTES['migration-studio'].label, icon: Wrench }],
    },
];
