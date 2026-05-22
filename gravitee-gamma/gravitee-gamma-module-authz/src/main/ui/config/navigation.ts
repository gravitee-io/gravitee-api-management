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
import { Brain, Globe, ShieldCheck, SlidersHorizontal } from 'lucide-react';
import { ROUTES } from './routes';

/**
 * Sidebar IA. Exposes Policy Management only. Policy structure
 * (entities · actions · schema) is introduced incrementally by
 * follow-up PRs in the gamma-ui stack.
 */
export const NAV_GROUPS: NavGroup[] = [
    {
        label: 'Policy Management',
        items: [
            { key: 'mcps', title: ROUTES.mcps.label, icon: ShieldCheck },
            { key: 'llms', title: ROUTES.llms.label, icon: Brain },
            { key: 'apis', title: ROUTES.apis.label, icon: Globe },
            { key: 'custom-policies', title: ROUTES['custom-policies'].label, icon: SlidersHorizontal },
        ],
    },
];
