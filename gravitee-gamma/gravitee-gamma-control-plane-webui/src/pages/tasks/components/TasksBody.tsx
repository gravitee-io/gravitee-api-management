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
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { AlertCircleIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { TaskList } from './TaskList';
import { TaskToolbar, type TaskFilterValue } from './TaskToolbar';
import { countByCategory, sortTasks, type TaskSortOrder } from '../tasks.mapping';
import type { TaskView } from '../tasks.types';

interface TasksBodyProps {
    readonly tasks: readonly TaskView[];
    readonly loading: boolean;
    readonly error: Error | null;
    readonly reload: () => void;
    readonly compact?: boolean;
    readonly onNavigate?: () => void;
}

export function TasksBody({ tasks, loading, error, reload, compact = false, onNavigate }: TasksBodyProps) {
    const [filter, setFilter] = useState<TaskFilterValue>('all');
    const [sort, setSort] = useState<TaskSortOrder>('newest');

    const counts = useMemo(() => countByCategory(tasks), [tasks]);
    const visibleTasks = useMemo(() => {
        const filtered = filter === 'all' ? tasks : tasks.filter(task => task.category === filter);
        return sortTasks(filtered, sort);
    }, [tasks, filter, sort]);

    if (error && tasks.length === 0) {
        return (
            <Alert className="border-destructive/50 bg-destructive/5">
                <AlertCircleIcon className="size-4 text-destructive" aria-hidden />
                <AlertDescription className="flex items-center gap-3 text-destructive">
                    <span className="flex-1">Failed to load tasks: {error.message}</span>
                    <Button variant="outline" size="sm" onClick={reload}>
                        <RefreshCwIcon aria-hidden />
                        Retry
                    </Button>
                </AlertDescription>
            </Alert>
        );
    }

    return (
        <div className="space-y-4">
            <div className={compact ? 'sticky top-0 z-10 border-b bg-background pb-3 pt-2' : undefined}>
                <TaskToolbar
                    counts={counts}
                    total={tasks.length}
                    filter={filter}
                    onFilterChange={setFilter}
                    sort={sort}
                    onSortChange={setSort}
                    compact={compact}
                />
            </div>
            <TaskList tasks={visibleTasks} loading={loading} onNavigate={onNavigate} />
        </div>
    );
}
