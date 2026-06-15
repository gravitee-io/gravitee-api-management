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
import { cn } from '@gravitee/graphene-core';
import {
    BrainCircuitIcon,
    LayoutDashboardIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    UsersIcon,
    UsersRoundIcon,
} from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';
import { NavLink } from 'react-router-dom';

interface NavItem {
    path: string;
    label: string;
    icon: ComponentType<{ className?: string }>;
    end?: boolean;
}

interface NavGroup {
    label: string;
    items: NavItem[];
}

const AI_PRODUCT_NAV_GROUPS: NavGroup[] = [
    {
        label: 'General',
        items: [
            { path: 'overview', label: 'Overview', icon: LayoutDashboardIcon },
            { path: 'general', label: 'General', icon: SlidersHorizontalIcon },
            { path: 'components', label: 'Components', icon: BrainCircuitIcon },
        ],
    },
    {
        // Plans = tiers (defaults + security + validation). Subscribers = portal access requests,
        // each approved with its own budget + rate limit (via subscription metadata).
        label: 'Consumer Access',
        items: [
            { path: 'plans', label: 'Plans', icon: ShieldIcon, end: false },
            { path: 'users', label: 'Subscribers', icon: UsersRoundIcon, end: false },
        ],
    },
    {
        label: 'Security',
        items: [{ path: 'user-permissions', label: 'User Permissions', icon: UsersIcon }],
    },
];

export function AiProductSidebarNav({ basePath }: { basePath: string }) {
    return (
        <div className="space-y-0.5 px-2 py-2">
            {AI_PRODUCT_NAV_GROUPS.map(group => (
                <div key={group.label} className="pt-4 first:pt-0">
                    <p className="mb-1 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{group.label}</p>
                    {group.items.map(item => {
                        const Icon = item.icon;
                        return (
                            <NavLink
                                end={item.end ?? true}
                                key={item.path}
                                to={`${basePath}/${item.path}`}
                                className={({ isActive }) =>
                                    cn(
                                        'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                                        isActive
                                            ? 'bg-accent text-foreground font-medium'
                                            : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                                    )
                                }
                            >
                                <Icon className="size-4 shrink-0" aria-hidden />
                                {item.label}
                            </NavLink>
                        );
                    })}
                </div>
            ))}
        </div>
    );
}
