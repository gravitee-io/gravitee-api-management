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

import { TasksBody } from './components/TasksBody';
import { TaskStatCards, type StatValue } from './components/TaskStatCards';
import type { HumanApprovals } from './humanApprovals';
import { countByCategory } from './tasks.mapping';
import { useTasks } from './useTasks';

function approvalsStat(humanApprovals: HumanApprovals): StatValue | undefined {
    switch (humanApprovals.status) {
        case 'off':
            return undefined;
        case 'loading':
            return null;
        case 'failed':
            return 'unavailable';
        case 'loaded':
            return humanApprovals.totalCount;
    }
}

/** The approvals are part of the total, so it is known only once they are. */
function totalStat(loading: boolean, humanApprovals: HumanApprovals, totalCount: number): StatValue {
    if (loading || humanApprovals.status === 'loading') {
        return null;
    }
    return humanApprovals.status === 'failed' ? 'unavailable' : totalCount;
}

export function TasksPage() {
    const {
        tasks,
        totalCount,
        loading,
        error,
        humanApprovals,
        approvalsOverflow,
        approvalsEnvironmentName,
        refreshing,
        reload,
        processPromotion,
    } = useTasks();

    const counts = useMemo(() => countByCategory(tasks), [tasks]);
    const reviews = counts.API_REVIEW + counts.CHANGES_REQUESTED;

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">Tasks &amp; Approvals</h1>
                    <p className="text-sm text-muted-foreground">Review pending tasks and approvals across your Gravitee products.</p>
                </div>
                <Button variant="outline" size="icon" aria-label="Refresh" disabled={refreshing} onClick={reload}>
                    <RefreshCwIcon aria-hidden className={refreshing ? 'animate-spin' : undefined} />
                </Button>
            </div>

            <TaskStatCards
                approvals={approvalsStat(humanApprovals)}
                approvalsEnvironmentName={approvalsEnvironmentName}
                total={totalStat(loading, humanApprovals, totalCount)}
                subscriptions={loading ? null : counts.SUBSCRIPTION}
                reviews={loading ? null : reviews}
                promotions={loading ? null : counts.API_PROMOTION}
            />

            <TasksBody
                tasks={tasks}
                loading={loading}
                error={error}
                humanApprovals={humanApprovals}
                approvalsOverflow={approvalsOverflow}
                reload={reload}
                onProcessPromotion={processPromotion}
            />
        </div>
    );
}
