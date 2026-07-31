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

import { TasksBody } from './components/TasksBody';
import { TaskStatCards } from './components/TaskStatCards';
import { countByCategory } from './tasks.mapping';
import { useTasks } from './useTasks';

export function TasksPage() {
    const { tasks, totalCount, loading, error, reload } = useTasks();

    const counts = useMemo(() => countByCategory(tasks), [tasks]);
    const initialLoading = loading && tasks.length === 0;
    const reviews = counts.API_REVIEW + counts.CHANGES_REQUESTED;

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">Tasks &amp; Approvals</h1>
                <p className="text-sm text-muted-foreground">Review pending tasks and approvals across your Gravitee products.</p>
            </div>

            <TaskStatCards
                total={initialLoading ? null : totalCount}
                subscriptions={initialLoading ? null : counts.SUBSCRIPTION}
                reviews={initialLoading ? null : reviews}
                promotions={initialLoading ? null : counts.API_PROMOTION}
            />

            <TasksBody tasks={tasks} loading={loading} error={error} reload={reload} />
        </div>
    );
}
