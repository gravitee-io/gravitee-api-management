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
import { NavLink, Outlet } from 'react-router-dom';

const TABS = [
    { to: '.', end: true, label: 'My alerts' },
    { to: 'activity', end: false, label: 'Activity' },
] as const;

/**
 * Sibling tabs for the env alerts list and activity board.
 * Create/edit routes sit outside this layout so the form stays a full page.
 */
export function AlertsLayout() {
    return (
        <div className="space-y-6">
            <div className="border-b">
                <nav className="flex items-center gap-6" aria-label="Alerts sections">
                    {TABS.map(tab => (
                        <NavLink
                            key={tab.label}
                            to={tab.to}
                            end={tab.end}
                            className={({ isActive }) =>
                                cn(
                                    '-mb-px border-b-2 px-0.5 pb-3 text-sm transition-colors',
                                    isActive
                                        ? 'border-foreground font-semibold text-foreground'
                                        : 'border-transparent text-muted-foreground hover:text-foreground',
                                )
                            }
                        >
                            {tab.label}
                        </NavLink>
                    ))}
                </nav>
            </div>
            <Outlet />
        </div>
    );
}
