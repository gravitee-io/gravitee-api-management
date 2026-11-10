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
    BoxesIcon,
    LayoutDashboardIcon,
    RocketIcon,
    ServerIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    UsersIcon,
    UsersRoundIcon,
    WorkflowIcon,
} from '@gravitee/graphene-core/icons';
import { useMemo, type ComponentType } from 'react';
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

const POLICY_STUDIO_PATH = 'policy-studio';

/**
 * An API Product is not an API, but it borrows the canonical API-detail vocabulary (FOUND-304) so a
 * user moving between the two reads the same words for the same thing: Settings, Subscriptions,
 * Sharding Tags, and User Permissions in General rather than in a group of its own.
 */
const API_PRODUCT_NAV_GROUPS: NavGroup[] = [
    {
        label: 'General',
        items: [
            { path: 'overview', label: 'Overview', icon: LayoutDashboardIcon },
            { path: 'general', label: 'Settings', icon: SlidersHorizontalIcon },
            { path: 'user-permissions', label: 'User Permissions', icon: UsersIcon },
            { path: 'apis', label: 'APIs', icon: BoxesIcon },
        ],
    },
    {
        label: 'Consumers',
        items: [
            { path: 'plans', label: 'Plans', icon: ShieldIcon },
            { path: POLICY_STUDIO_PATH, label: 'Policy Studio', icon: WorkflowIcon },
            { path: 'consumers', label: 'Subscriptions', icon: UsersRoundIcon, end: false },
        ],
    },
    {
        label: 'Operations',
        items: [{ path: 'deployment/configuration', label: 'Sharding Tags', icon: RocketIcon, end: false }],
    },
];

interface ApiProductSidebarNavProps {
    basePath: string;
    /** DEFINITION read, once the permissions are loaded: shows Resources and the Policy Studio. */
    showDefinitionNav?: boolean;
}

const RESOURCES_GROUP: NavGroup = {
    label: 'Design',
    items: [{ path: 'resources', label: 'Resources', icon: ServerIcon }],
};

export function ApiProductSidebarNav({ basePath, showDefinitionNav = false }: ApiProductSidebarNavProps) {
    const navGroups = useMemo((): NavGroup[] => {
        if (!showDefinitionNav) {
            return API_PRODUCT_NAV_GROUPS.map(group => ({ ...group, items: group.items.filter(item => item.path !== POLICY_STUDIO_PATH) }));
        }
        const [general, ...rest] = API_PRODUCT_NAV_GROUPS;
        return [general, RESOURCES_GROUP, ...rest];
    }, [showDefinitionNav]);

    return (
        <div className="space-y-0.5 px-2 py-2">
            {navGroups.map(group => (
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
