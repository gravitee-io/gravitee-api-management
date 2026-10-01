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
import { cn } from '@gravitee/graphene-core';
import { LockIcon } from '@gravitee/graphene-core/icons';
import { NavLink } from 'react-router-dom';

import { CLOUD_SETTINGS_NAV_GROUPS } from '../cloud-settings-navigation';

interface CloudSettingsSidebarNavProps {
    readonly basePath: string;
}

export function CloudSettingsSidebarNav({ basePath }: CloudSettingsSidebarNavProps) {
    return (
        <div className="space-y-0.5 px-2 py-2">
            {CLOUD_SETTINGS_NAV_GROUPS.map(group => (
                <div key={group.label} className="pt-4 first:pt-0">
                    <p className="mb-1 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{group.label}</p>
                    {group.items.map(item => (
                        <NavLink
                            end
                            key={item.key}
                            to={`${basePath}/${item.key}`}
                            data-testid={`cloud-settings-nav-${item.key}`}
                            className={({ isActive }) =>
                                cn(
                                    'flex items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm transition-colors',
                                    isActive
                                        ? 'bg-accent text-foreground font-medium'
                                        : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                                )
                            }
                        >
                            <span>{item.label}</span>
                            {item.locked ? <LockIcon className="size-3.5 shrink-0 opacity-70" aria-label="Enterprise feature" /> : null}
                        </NavLink>
                    ))}
                </div>
            ))}
        </div>
    );
}
