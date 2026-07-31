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
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

import { TaskRow } from './TaskRow';
import type { TaskView } from '../tasks.types';

function TaskListSkeleton() {
    return (
        <div className="space-y-3" aria-busy="true">
            {[0, 1, 2].map(i => (
                <Card key={i}>
                    <CardContent className="flex items-start gap-4">
                        <div className="size-10 shrink-0 animate-pulse rounded-lg bg-muted" />
                        <div className="flex-1 space-y-2 pt-1">
                            <div className="h-4 w-40 animate-pulse rounded bg-muted" />
                            <div className="h-3 w-64 animate-pulse rounded bg-muted" />
                            <div className="h-7 w-28 animate-pulse rounded bg-muted" />
                        </div>
                    </CardContent>
                </Card>
            ))}
        </div>
    );
}

export function TaskList({ tasks, loading, onNavigate }: { tasks: readonly TaskView[]; loading: boolean; onNavigate?: () => void }) {
    if (loading && tasks.length === 0) {
        return <TaskListSkeleton />;
    }

    if (tasks.length === 0) {
        return (
            <Card>
                <CardContent className="flex flex-col items-center gap-2 py-12 text-center">
                    <CircleCheckIcon className="size-6 text-muted-foreground" aria-hidden />
                    <p className="text-sm font-medium">No pending tasks</p>
                    <p className="text-xs text-muted-foreground">You are all caught up.</p>
                </CardContent>
            </Card>
        );
    }

    return (
        <div className="space-y-3">
            {tasks.map(task => (
                <TaskRow key={task.id} task={task} onNavigate={onNavigate} />
            ))}
        </div>
    );
}
