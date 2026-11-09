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
import { Link } from 'react-router-dom';

import { TaskList } from './TaskList';
import { TaskToolbar, type TaskFilterValue } from './TaskToolbar';
import type { HumanApprovals } from '../humanApprovals';
import { countByCategory, sortTasks, type TaskSortOrder } from '../tasks.mapping';
import type { TaskView } from '../tasks.types';
import type { ApprovalsOverflow } from '../useTasks';

function LoadFailedAlert({ messages, onRetry }: { messages: readonly string[]; onRetry: () => void }) {
    return (
        <Alert className="border-destructive/50 bg-destructive/5">
            <AlertCircleIcon className="size-4 text-destructive" aria-hidden />
            <AlertDescription className="flex items-center gap-3 text-destructive">
                <span className="flex-1 space-y-1">
                    {messages.map(message => (
                        <span key={message} className="block">
                            {message}
                        </span>
                    ))}
                </span>
                <Button variant="outline" size="sm" onClick={onRetry}>
                    <RefreshCwIcon aria-hidden />
                    Retry
                </Button>
            </AlertDescription>
        </Alert>
    );
}

function describeOverflow({ listed, total, environmentName }: ApprovalsOverflow): string {
    const where = environmentName ? ` in ${environmentName}` : '';
    if (listed === 0) {
        const [noun, verb] = total === 1 ? ['human approval', 'waits'] : ['human approvals', 'wait'];
        return `${total.toLocaleString()} ${noun}${where} ${verb} in the inbox.`;
    }
    return `Showing ${listed.toLocaleString()} of ${total.toLocaleString()} human approvals${where}.`;
}

interface TasksBodyProps {
    readonly tasks: readonly TaskView[];
    readonly loading: boolean;
    readonly error: Error | null;
    readonly humanApprovals: HumanApprovals;
    readonly approvalsOverflow: ApprovalsOverflow | null;
    readonly reload: () => void;
    readonly compact?: boolean;
    readonly onNavigate?: () => void;
    readonly onProcessPromotion?: (promotionId: string, accepted: boolean) => Promise<void>;
}

export function TasksBody({
    tasks,
    loading,
    error,
    humanApprovals,
    approvalsOverflow,
    reload,
    compact = false,
    onNavigate,
    onProcessPromotion,
}: TasksBodyProps) {
    const [filter, setFilter] = useState<TaskFilterValue>('all');
    const [sort, setSort] = useState<TaskSortOrder>('newest');

    // The sheet stays open across environments, and the Approval chip goes until approvals are read: nothing else could clear it.
    const activeFilter = filter === 'HUMAN_APPROVAL' && humanApprovals.status !== 'loaded' ? 'all' : filter;

    const counts = useMemo(() => countByCategory(tasks), [tasks]);
    const visibleTasks = useMemo(() => {
        const filtered = activeFilter === 'all' ? tasks : tasks.filter(task => task.category === activeFilter);
        return sortTasks(filtered, sort);
    }, [tasks, activeFilter, sort]);

    // Approvals alone on screen must not pass for "nothing else to do" when the other tasks could not be read.
    const userTasksFailed = error !== null && tasks.every(task => task.category === 'HUMAN_APPROVAL');
    const approvalsRefreshFailed = humanApprovals.status === 'loaded' && humanApprovals.refreshError !== undefined;
    const failures = [
        userTasksFailed ? `Failed to load tasks: ${error.message}` : null,
        humanApprovals.status === 'failed' ? `Failed to load human approvals: ${humanApprovals.error.message}` : null,
        approvalsRefreshFailed ? 'Could not refresh the human approvals: they may be out of date.' : null,
    ].filter(message => message !== null);
    const anyFailed = failures.length > 0;
    // The chips count what the cards count: every approval the server holds, not only those listed.
    const approvalsTotal = humanApprovals.status === 'loaded' ? humanApprovals.totalCount : 0;
    const chipCounts = { ...counts, HUMAN_APPROVAL: approvalsTotal };

    return (
        <div className="space-y-4">
            {anyFailed && <LoadFailedAlert messages={failures} onRetry={reload} />}
            {(tasks.length > 0 || !anyFailed) && (
                <div className={compact ? 'sticky top-0 z-10 border-b bg-background pb-3 pt-2' : undefined}>
                    <TaskToolbar
                        counts={chipCounts}
                        total={tasks.length - counts.HUMAN_APPROVAL + approvalsTotal}
                        approvalsLoaded={humanApprovals.status === 'loaded'}
                        filter={activeFilter}
                        onFilterChange={setFilter}
                        sort={sort}
                        onSortChange={setSort}
                        compact={compact}
                    />
                </div>
            )}
            {approvalsOverflow && (
                <p className="text-sm text-muted-foreground">
                    {describeOverflow(approvalsOverflow)}{' '}
                    <Link to={approvalsOverflow.inboxPath} onClick={onNavigate} className="text-primary hover:underline">
                        Open the inbox
                    </Link>
                </p>
            )}
            {/* The list's empty state says all is done, which neither a failed read nor approvals left in the inbox can tell. */}
            {(visibleTasks.length > 0 || !(anyFailed || approvalsOverflow)) && (
                <TaskList
                    tasks={visibleTasks}
                    loading={loading || humanApprovals.status === 'loading'}
                    onNavigate={onNavigate}
                    onProcessPromotion={onProcessPromotion}
                />
            )}
        </div>
    );
}
