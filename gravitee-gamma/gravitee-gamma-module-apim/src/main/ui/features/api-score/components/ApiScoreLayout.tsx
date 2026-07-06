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

const TABS: { to: string; label: string; end: boolean }[] = [
    { to: '.', label: 'Overview', end: true },
    { to: 'rulesets', label: 'Rulesets & Functions', end: false },
];

export function ApiScoreLayout() {
    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">API Score</h1>
                <p className="text-sm text-muted-foreground">Get personalized recommendations to enhance your API&apos;s quality.</p>
            </div>

            <nav className="flex gap-1 border-b" aria-label="API Score sections">
                {TABS.map(tab => (
                    <NavLink
                        key={tab.to}
                        to={tab.to}
                        end={tab.end}
                        className={({ isActive }) =>
                            cn(
                                'border-b-2 -mb-px px-3 py-2.5 text-sm font-medium transition-colors',
                                isActive ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground',
                            )
                        }
                    >
                        {tab.label}
                    </NavLink>
                ))}
            </nav>

            <Outlet />
        </div>
    );
}
