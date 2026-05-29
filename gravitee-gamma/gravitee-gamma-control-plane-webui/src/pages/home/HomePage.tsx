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
import { useMemo } from 'react';

import { ApplicationCard } from './components/application-card/ApplicationCard';
import { APPLICATIONS, buildModulePath, type ModuleId } from './components/application-card/applications';
import { GET_STARTED_STEPS } from './components/get-started/get-started';
import { GetStartedCard } from './components/get-started/GetStartedCard';
import { useAgentCount, useApiCount } from './useModuleCounts';
import { useUser } from '../../features/auth';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { useEnvHrid } from '../../features/environment/environment.utils';
import type { GammaModule } from '../../features/modules';

/** Format a live count into a card badge, e.g. (24, 'API', 'APIs') -> '24 APIs'. */
function formatCountBadge(count: number | null, singular: string, plural: string): string | undefined {
    if (count === null) return undefined;
    return `${count} ${count === 1 ? singular : plural}`;
}

interface HomePageProps {
    readonly modules: readonly GammaModule[];
    readonly loading: boolean;
    readonly error: Error | null;
}

export function HomePage({ modules, loading, error }: HomePageProps) {
    const user = useUser();
    const envHrid = useEnvHrid();
    const envName = useEnvironmentStore(s => s.currentEnvironment?.name);
    const firstName = user?.firstname?.trim() || user?.displayName?.split(' ')[0] || '';

    const availableModuleIds = useMemo(() => new Set(modules.map(m => m.id)), [modules]);
    const isAvailable = (moduleId: ModuleId) => availableModuleIds.has(moduleId);

    const apiCount = useApiCount({ enabled: !loading && availableModuleIds.has('apim') });
    const agentCount = useAgentCount({ enabled: !loading && availableModuleIds.has('aim') });
    const dynamicBadges: Partial<Record<ModuleId, string>> = {
        apim: formatCountBadge(apiCount, 'API', 'APIs'),
        aim: formatCountBadge(agentCount, 'agent', 'agents'),
    };

    return (
        <div className="space-y-8">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Welcome back{firstName ? `, ${firstName}` : ''}</h1>
                <h4 className="text-muted-foreground">Your {envName || 'current'} environment overview</h4>
            </div>

            <section aria-labelledby="get-started-heading" className="space-y-3">
                <h5 id="get-started-heading" className="text-sm font-semibold tracking-tight">
                    Get Started with Gravitee
                </h5>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {GET_STARTED_STEPS.map(step => (
                        <GetStartedCard
                            key={step.title}
                            step={step}
                            to={`${buildModulePath(envHrid, step.moduleId)}/${step.subPath}`}
                        />
                    ))}
                </div>
            </section>

            <section aria-labelledby="applications-heading" className="space-y-3">
                <div className="space-y-0.5">
                    <h5 id="applications-heading" className="text-sm font-semibold tracking-tight">
                        Applications
                    </h5>
                    <p className="text-xs text-muted-foreground">Navigate to any Gravitee product</p>
                </div>

                {error ? (
                    <p role="alert" className="text-sm text-destructive">
                        Failed to load modules: {error.message}
                    </p>
                ) : loading ? (
                    <div className="grid grid-cols-1 gap-4 grid-cols-2 md:grid-cols-4" aria-busy="true">
                        {APPLICATIONS.map(app => (
                            <div key={app.title} className="rounded-xl border bg-card p-5 opacity-60">
                                <div className="h-24 animate-pulse rounded bg-muted" aria-hidden />
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 gap-4 grid-cols-2 md:grid-cols-4">
                        {APPLICATIONS.map(app => {
                            const to = isAvailable(app.moduleId) ? buildModulePath(envHrid, app.moduleId) : null;
                            const liveBadge = dynamicBadges[app.moduleId];
                            const resolvedApp = liveBadge !== undefined ? { ...app, badge: liveBadge } : app;
                            return <ApplicationCard key={app.title} app={resolvedApp} to={to} />;
                        })}
                    </div>
                )}
            </section>
        </div>
    );
}
