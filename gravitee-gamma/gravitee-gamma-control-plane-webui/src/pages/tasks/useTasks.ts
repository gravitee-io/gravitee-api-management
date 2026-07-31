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

import { toTaskView } from './tasks.mapping';
import type { TasksResponse, TaskView } from './tasks.types';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { getPrimaryHrid, useEnvHrid } from '../../features/environment/environment.utils';
import { managementApi } from '../../shared/api/api-client';

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

export interface UseTasksResult {
    readonly tasks: TaskView[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly error: Error | null;
    readonly reload: () => void;
}

function useTasksResponse({ poll }: { poll: boolean }): {
    response: TasksResponse | null;
    loading: boolean;
    error: Error | null;
    reload: () => void;
} {
    const [response, setResponse] = useState<TasksResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);
    const [tick, setTick] = useState(0);
    const intervalMs = useTasksPollIntervalMs(poll);

    const reload = useCallback(() => setTick(t => t + 1), []);

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

    return { response, loading, error, reload };
}

export function useTasks(): UseTasksResult {
    const envHrid = useEnvHrid();
    const environments = useEnvironmentStore(s => s.environments);
    const { response, loading, error, reload } = useTasksResponse({ poll: false });

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

    const tasks = useMemo(
        () => (response ? response.data.map(entity => toTaskView(entity, response.metadata, resolveEnvHrid)) : []),
        [response, resolveEnvHrid],
    );

    const totalCount = response?.page?.total_elements ?? tasks.length;

    return { tasks, totalCount, loading, error, reload };
}

export function usePendingTaskCount(): number | null {
    const { response } = useTasksResponse({ poll: true });
    if (!response) {
        return null;
    }
    return response.page?.total_elements ?? response.data.length;
}
