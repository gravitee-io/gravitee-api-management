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
import type { NavGroup } from '@gravitee/graphene-core';
import { ClipboardCheck, Home } from 'lucide-react';

import { HOME_NAV_KEY, HOST_NAV_LABELS, type HostNavKey, TASKS_NAV_KEY } from './routes';

export function buildNavGroups(hrefFor: (key: HostNavKey) => string): NavGroup[] {
    return [
        {
            label: 'Overview',
            items: [
                { key: HOME_NAV_KEY, title: HOST_NAV_LABELS.home, icon: Home, href: hrefFor(HOME_NAV_KEY) },
                { key: TASKS_NAV_KEY, title: HOST_NAV_LABELS.tasks, icon: ClipboardCheck, href: hrefFor(TASKS_NAV_KEY) },
            ],
        },
    ];
}
