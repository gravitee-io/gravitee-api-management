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

import { Button } from '@gravitee/graphene-core';
import { RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { ApplicationCard, type CardMetrics } from './components/application-card/ApplicationCard';
import { APPLICATIONS, buildModulePath, type ModuleId } from './components/application-card/applications';
import { GET_STARTED_STEPS } from './components/get-started/get-started';
import { GetStartedCard } from './components/get-started/GetStartedCard';
import {
    useActiveAppCount,
    useAgentCount,
    useApiCount,
    useDeployedPolicyCount,
    useDeviceCount,
    useMcpServerCount,
    usePrincipalCount,
} from './useModuleMetrics';
import { useUser } from '../../features/auth';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { useEnvHrid } from '../../features/environment/environment.utils';
import type { GammaModule } from '../../features/modules';

function pluralize(count: number | null, singular: string, plural: string): string {
    return count === 1 ? singular : plural;
}

interface HomePageProps {
    readonly modules: readonly GammaModule[];
    readonly loading: boolean;
    readonly error: Error | null;
    readonly onRetry?: () => void;
}

export function HomePage({ modules, loading, error, onRetry }: HomePageProps) {
    const user = useUser();
    const envHrid = useEnvHrid();
    const envName = useEnvironmentStore(s => s.currentEnvironment?.name);
    const firstName = user?.firstname?.trim() || user?.displayName?.split(' ')[0] || '';

    const availableModuleIds = useMemo(() => new Set(modules.map(m => m.id)), [modules]);
    const isAvailable = (moduleId: ModuleId) => availableModuleIds.has(moduleId);

    const apiCount = useApiCount({ enabled: !loading && isAvailable('apim') });
    const agentCount = useAgentCount({ enabled: !loading && isAvailable('aim') });
    const mcpServerCount = useMcpServerCount({ enabled: !loading && isAvailable('aim') });
    const appCount = useActiveAppCount({ enabled: !loading && isAvailable('platform') });
    const policyCount = useDeployedPolicyCount({ enabled: !loading && isAvailable('authz') });
    const principalCount = usePrincipalCount({ enabled: !loading && isAvailable('authz') });
    const deviceCount = useDeviceCount({ enabled: !loading && isAvailable('edge') });

    const moduleMetrics: Partial<Record<ModuleId, CardMetrics>> = {
        apim: {
            primary: { value: apiCount, label: pluralize(apiCount, 'API', 'APIs') },
        },
        aim: {
            primary: { value: agentCount, label: pluralize(agentCount, 'agent', 'agents') },
            secondary: { value: mcpServerCount, label: pluralize(mcpServerCount, 'MCP server', 'MCP servers') },
        },
        platform: {
            primary: { value: appCount, label: pluralize(appCount, 'active app', 'active apps') },
        },
        authz: {
            primary: { value: policyCount, label: pluralize(policyCount, 'policy deployed', 'policies deployed') },
            secondary: { value: principalCount, label: pluralize(principalCount, 'principal', 'principals') },
        },
        edge: {
            primary: { value: deviceCount, label: pluralize(deviceCount, 'active device (24h)', 'active devices (24h)') },
        },
    };

    const getStartedSteps = useMemo(() => GET_STARTED_STEPS.filter(step => availableModuleIds.has(step.moduleId)), [availableModuleIds]);

    return (
        <div className="max-w-screen-xl space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">Welcome back{firstName ? `, ${firstName}` : ''}</h1>
                <p className="text-sm text-muted-foreground">Your {envName || 'current'} environment overview</p>
            </div>

            {!loading && getStartedSteps.length > 0 && (
                <section aria-labelledby="get-started-heading">
                    <div className="space-y-3 rounded-xl border border-border/50 bg-muted/40 p-6">
                        <h2 id="get-started-heading" className="text-base font-semibold tracking-tight">
                            Get Started with Gravitee
                        </h2>
                        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                            {getStartedSteps.map(step => (
                                <GetStartedCard
                                    key={step.title}
                                    step={step}
                                    to={`${buildModulePath(envHrid, step.moduleId)}/${step.subPath}`}
                                />
                            ))}
                        </div>
                    </div>
                </section>
            )}

            <section aria-labelledby="applications-heading" className="space-y-3">
                <div className="space-y-0.5">
                    <h2 id="applications-heading" className="text-base font-semibold tracking-tight">
                        Applications
                    </h2>
                    <p className="text-xs text-muted-foreground">Navigate to any Gravitee product</p>
                </div>

                {error ? (
                    <div
                        role="alert"
                        className="flex items-center gap-3 rounded-lg border border-destructive/50 bg-destructive/5 px-4 py-3"
                    >
                        <p className="flex-1 text-sm text-destructive">Failed to load modules: {error.message}</p>
                        {onRetry && (
                            <Button variant="outline" size="sm" onClick={onRetry}>
                                <RefreshCwIcon aria-hidden />
                                Retry
                            </Button>
                        )}
                    </div>
                ) : loading ? (
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-busy="true">
                        {APPLICATIONS.map(app => (
                            <div key={app.title} className="flex flex-col gap-3 rounded-xl border bg-card p-5 opacity-60">
                                <div className="size-12 animate-pulse rounded-lg bg-muted" aria-hidden />
                                <div className="h-4 w-24 animate-pulse rounded bg-muted" aria-hidden />
                                <div className="space-y-1.5 border-t border-border/50 pt-3">
                                    <div className="h-6 w-16 animate-pulse rounded bg-muted" aria-hidden />
                                    <div className="h-3 w-20 animate-pulse rounded bg-muted" aria-hidden />
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                        {APPLICATIONS.map(app => {
                            const to = isAvailable(app.moduleId) ? buildModulePath(envHrid, app.moduleId) : null;
                            return (
                                <ApplicationCard key={app.title} app={app} to={to} metrics={to ? moduleMetrics[app.moduleId] : undefined} />
                            );
                        })}
                    </div>
                )}
            </section>
        </div>
    );
}
