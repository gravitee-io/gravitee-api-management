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
import { Card, CardContent } from '@gravitee/graphene-core';
import { SparklesIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { ApplicationCard } from './components/application-card/ApplicationCard';
import { APPLICATIONS, buildModulePath, type ModuleId } from './components/application-card/applications';
import { NEXT_STEPS } from './components/next-steps/next-steps';
import { NextStepCard } from './components/next-steps/NextStepCard';
import { useAgentCount, useApiCount } from './useModuleCounts';
import { useUser } from '../../features/auth';
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
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Welcome back{firstName ? `, ${firstName}` : ''}</h1>
                <p className="text-sm text-muted-foreground">
                    Your Gravitee platform overview — manage APIs, events, agent access, and more from one place.
                </p>
            </div>

            <aside aria-labelledby="suggested-next-steps-heading">
                <Card className="bg-highlight/5 border-highlight/20">
                    <CardContent className="pt-5 pb-5 space-y-4">
                        <div className="flex items-start gap-3">
                            <div className="rounded-lg bg-highlight/10 p-2 shrink-0">
                                <SparklesIcon className="size-5 text-highlight" aria-hidden />
                            </div>
                            <div className="space-y-0.5">
                                <h2 id="suggested-next-steps-heading" className="text-sm font-semibold leading-tight">
                                    Suggested next steps
                                </h2>
                                <p className="text-xs text-muted-foreground">
                                    Based on your recent activity, here are some actions you might want to take.
                                </p>
                            </div>
                        </div>
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
                            {NEXT_STEPS.map(step => (
                                <NextStepCard key={step.title} step={step} />
                            ))}
                        </div>
                    </CardContent>
                </Card>
            </aside>

            <div className="space-y-3">
                <div className="space-y-0.5">
                    <h2 className="text-xl font-semibold tracking-tight">Applications</h2>
                    <p className="text-sm text-muted-foreground">Navigate to any Gravitee product</p>
                </div>

                {/* Loading / error / data states (review #5). While `useGammaModules` is in
                    flight, render a skeleton grid instead of "every card disabled" so the
                    UI doesn't flicker from "all locked" to "all active" once the fetch
                    resolves. */}
                {error ? (
                    <p role="alert" className="text-sm text-destructive">
                        Failed to load modules: {error.message}
                    </p>
                ) : loading ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" aria-busy="true">
                        {APPLICATIONS.map(app => (
                            <Card key={app.title} className="opacity-60">
                                <CardContent className="pt-5 pb-5">
                                    <div className="h-24 animate-pulse rounded bg-muted" aria-hidden />
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {APPLICATIONS.map(app => {
                            // Coming-soon and modules not returned by /modules render the same
                            // card visual but without a link or "Open →" CTA. The list itself
                            // stays static — the home is a product showcase, navigation comes
                            // from whatever the backend returns (same source as the app switcher).
                            if (app.kind === 'coming-soon') {
                                return <ApplicationCard key={app.title} app={app} to={null} />;
                            }
                            const to = isAvailable(app.moduleId) ? buildModulePath(envHrid, app.moduleId) : null;
                            const liveBadge = dynamicBadges[app.moduleId];
                            const resolvedApp = liveBadge !== undefined ? { ...app, badge: liveBadge } : app;
                            return <ApplicationCard key={app.title} app={resolvedApp} to={to} />;
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}
