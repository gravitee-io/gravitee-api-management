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
import { useCallback, useEffect, useMemo, useState } from 'react';

import { useHumanApprovals, type HumanApprovals } from './humanApprovals';
import { humanApprovalsInboxPath, humanApprovalToTaskView, toTaskView } from './tasks.mapping';
import type { TasksResponse, TaskView } from './tasks.types';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { getPrimaryHrid } from '../../features/environment/environment.utils';
import { useCurrentEnvHrid } from '../../features/environment/useCurrentEnvHrid';
import { managementApi, request } from '../../shared/api/api-client';

/**
 * `managementV2OrganizationApi.post` treats a falsy body (`false`, the reject payload) as "no
 * body" and drops it — call `request` directly so a raw boolean still gets serialized and sent.
 */
async function requestProcessPromotion(promotionId: string, accepted: boolean): Promise<void> {
    await request<unknown>('management-v2-organization', `/promotions/${encodeURIComponent(promotionId)}/_process`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(accepted),
    });
}

const DEFAULT_POLL_INTERVAL_MS = 10_000;

let cachedPollIntervalMs: number | null = null;

interface ConsoleSettingsResponse {
    scheduler?: { tasks?: number };
}

function useTasksPollIntervalMs(enabled: boolean): number {
    const [intervalMs, setIntervalMs] = useState<number>(cachedPollIntervalMs ?? DEFAULT_POLL_INTERVAL_MS);

    useEffect(() => {
        if (!enabled || cachedPollIntervalMs !== null) {
            return;
        }
        let cancelled = false;
        managementApi
            .get<ConsoleSettingsResponse>('/settings')
            .then(settings => {
                const seconds = settings?.scheduler?.tasks;
                cachedPollIntervalMs = typeof seconds === 'number' && seconds > 0 ? seconds * 1000 : DEFAULT_POLL_INTERVAL_MS;
                if (!cancelled) {
                    setIntervalMs(cachedPollIntervalMs);
                }
            })
            .catch(() => {
                cachedPollIntervalMs = DEFAULT_POLL_INTERVAL_MS;
            });
        return () => {
            cancelled = true;
        };
    }, [enabled]);

    return intervalMs;
}

export interface ApprovalsOverflow {
    readonly listed: number;
    readonly total: number;
    readonly inboxPath: string;
    readonly environmentName: string | undefined;
}

export interface UseTasksResult {
    readonly tasks: TaskView[];
    readonly totalCount: number;
    /** `/user/tasks` only: `humanApprovals` settles on its own. */
    readonly loading: boolean;
    readonly error: Error | null;
    readonly humanApprovals: HumanApprovals;
    /** Set when AIM holds more approvals than `tasks` lists; the rest are read in its inbox. */
    readonly approvalsOverflow: ApprovalsOverflow | null;
    /** The environment the approvals are read in, unlike the other tasks, which span every environment. */
    readonly approvalsEnvironmentName: string | undefined;
    readonly refreshing: boolean;
    readonly reload: () => void;
    readonly processPromotion: (promotionId: string, accepted: boolean) => Promise<void>;
}

function useTasksResponse({ poll }: { poll: boolean }): {
    response: TasksResponse | null;
    loading: boolean;
    reloading: boolean;
    error: Error | null;
    reload: () => void;
} {
    const [response, setResponse] = useState<TasksResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [reloading, setReloading] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const [tick, setTick] = useState(0);
    const intervalMs = useTasksPollIntervalMs(poll);

    const reload = useCallback(() => {
        setReloading(true);
        setTick(t => t + 1);
    }, []);

    useEffect(() => {
        let cancelled = false;

        const load = () =>
            managementApi
                .get<TasksResponse>('/user/tasks')
                .then(result => {
                    if (!cancelled) {
                        setResponse(result);
                        setError(null);
                    }
                })
                .catch(err => {
                    if (!cancelled) {
                        setError(err instanceof Error ? err : new Error(String(err)));
                    }
                })
                .finally(() => {
                    if (!cancelled) {
                        setLoading(false);
                        setReloading(false);
                    }
                });

        void load();

        const refreshIfVisible = () => {
            if (!document.hidden) {
                void load();
            }
        };
        document.addEventListener('visibilitychange', refreshIfVisible);
        window.addEventListener('focus', refreshIfVisible);

        const intervalId = poll ? setInterval(refreshIfVisible, intervalMs) : undefined;

        return () => {
            cancelled = true;
            document.removeEventListener('visibilitychange', refreshIfVisible);
            window.removeEventListener('focus', refreshIfVisible);
            if (intervalId !== undefined) {
                clearInterval(intervalId);
            }
        };
    }, [tick, poll, intervalMs]);

    return { response, loading, reloading, error, reload };
}

export function useTasks(): UseTasksResult {
    // The sheet also opens from an account module, whose address has no environment.
    const envHrid = useCurrentEnvHrid();
    const environments = useEnvironmentStore(s => s.environments);
    const environmentName = useEnvironmentStore(s => s.currentEnvironment?.name);
    const { response, loading, reloading, error, reload: reloadUserTasks } = useTasksResponse({ poll: false });
    const { humanApprovals, refreshing: refreshingHumanApprovals, refresh: refreshHumanApprovals } = useHumanApprovals();

    const resolveEnvHrid = useCallback(
        (environmentId?: string): string => {
            if (!environmentId) {
                return envHrid;
            }
            const env = environments.find(e => e.id === environmentId);
            return env ? getPrimaryHrid(env) : envHrid;
        },
        [environments, envHrid],
    );

    const userTasks = useMemo(
        () => (response ? response.data.map(entity => toTaskView(entity, response.metadata, resolveEnvHrid)) : []),
        [response, resolveEnvHrid],
    );

    const approvalTasks = useMemo(
        () =>
            humanApprovals.status === 'loaded'
                ? humanApprovals.items.map(approval => humanApprovalToTaskView(approval, resolveEnvHrid(humanApprovals.environmentId)))
                : [],
        [humanApprovals, resolveEnvHrid],
    );

    const tasks = useMemo(() => [...approvalTasks, ...userTasks], [approvalTasks, userTasks]);

    const approvalsCount = humanApprovals.status === 'loaded' ? humanApprovals.totalCount : 0;
    const totalCount = (response?.page?.total_elements ?? userTasks.length) + approvalsCount;

    const approvalsOverflow =
        humanApprovals.status === 'loaded' && humanApprovals.totalCount > humanApprovals.items.length
            ? {
                  listed: humanApprovals.items.length,
                  total: humanApprovals.totalCount,
                  inboxPath: humanApprovalsInboxPath(resolveEnvHrid(humanApprovals.environmentId)),
                  environmentName,
              }
            : null;

    const reload = useCallback(() => {
        reloadUserTasks();
        refreshHumanApprovals();
    }, [reloadUserTasks, refreshHumanApprovals]);

    const processPromotion = useCallback(
        async (promotionId: string, accepted: boolean) => {
            await requestProcessPromotion(promotionId, accepted);
            reloadUserTasks();
        },
        [reloadUserTasks],
    );

    return {
        tasks,
        totalCount,
        loading,
        error,
        humanApprovals,
        approvalsOverflow,
        approvalsEnvironmentName: environmentName,
        refreshing: reloading || refreshingHumanApprovals,
        reload,
        processPromotion,
    };
}

export function usePendingTaskCount(): number | null {
    const { response } = useTasksResponse({ poll: true });
    const { humanApprovals } = useHumanApprovals();
    const approvalsCount = humanApprovals.status === 'loaded' ? humanApprovals.totalCount : null;
    if (!response && approvalsCount === null) {
        return null;
    }
    return (response ? (response.page?.total_elements ?? response.data.length) : 0) + (approvalsCount ?? 0);
}
