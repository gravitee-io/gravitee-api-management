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
import { permissionService } from '@gravitee/gamma-modules-sdk';
import { cn, Skeleton } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { NavLink } from 'react-router-dom';

import type { ApplicationDetailNavGroup } from '../../../../config/applicationDetailNavigation';
import { filterApplicationDetailNavGroups } from '../../../../config/applicationDetailNavigation';
import { usePermissionServiceSnapshot } from '../../../shared/hooks/usePermissionServiceSnapshot';

export type { ApplicationDetailNavGroup, ApplicationDetailNavItem } from '../../../../config/applicationDetailNavigation';
export { APPLICATION_NAV_GROUPS } from '../../../../config/applicationDetailNavigation';

interface ApplicationDetailSidebarNavProps {
    readonly groups: ApplicationDetailNavGroup[];
    readonly basePath: string;
    /** When false, renders a loading skeleton in place of nav items. */
    readonly permissionsReady?: boolean;
}

export function ApplicationDetailSidebarNav({ groups, basePath, permissionsReady = true }: ApplicationDetailSidebarNavProps) {
    const permissionVersion = usePermissionServiceSnapshot();

    const visibleGroups = useMemo(() => {
        if (!permissionsReady) {
            return [];
        }
        return filterApplicationDetailNavGroups(groups, permissions => permissionService.hasAnyOf(permissions));
    }, [groups, permissionsReady, permissionVersion]);

    if (!permissionsReady) {
        return (
            <div className="space-y-0.5 px-2 py-4">
                {Array.from({ length: 5 }).map((_, index) => (
                    <div key={index} className="px-3 py-2">
                        <Skeleton className="h-4 rounded" />
                    </div>
                ))}
            </div>
        );
    }

    if (visibleGroups.length === 0) {
        return (
            <p className="px-3 py-4 text-xs text-muted-foreground">
                No sections are available with your current permissions for this application.
            </p>
        );
    }

    return (
        <div className="space-y-0.5 px-2 py-2">
            {visibleGroups.map(group => (
                <div key={group.label} className="pt-4 first:pt-0">
                    <p className="mb-1 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{group.label}</p>
                    {group.items.map(item => {
                        const Icon = item.icon;
                        return (
                            <NavLink
                                end
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
