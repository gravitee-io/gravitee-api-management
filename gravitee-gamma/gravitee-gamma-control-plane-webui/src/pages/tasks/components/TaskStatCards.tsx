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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { CloudUploadIcon, EyeIcon, InboxIcon, KeyIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

/** `null` is still loading; `unavailable` could not be read, which no number or endless skeleton would say. */
export type StatValue = number | null | 'unavailable';

function StatCard({ Icon, title, total }: { Icon: LucideIcon; title: string; total: StatValue }) {
    return (
        <Card className="flex-1">
            <CardContent className="pt-5 pb-5">
                <div className="flex items-center gap-2 mb-3">
                    <div className="rounded-lg bg-primary/10 p-2">
                        <Icon className="size-4 text-primary" aria-hidden />
                    </div>
                    <p className="text-sm font-medium text-muted-foreground">{title}</p>
                </div>
                {total === null ? (
                    <Skeleton className="h-7 w-14 rounded" />
                ) : total === 'unavailable' ? (
                    <p className="text-2xl font-semibold tracking-tight text-muted-foreground">
                        <span aria-hidden>—</span>
                        <span className="sr-only">{title} unavailable</span>
                    </p>
                ) : (
                    <p className="text-2xl font-semibold tracking-tight">{total.toLocaleString()}</p>
                )}
            </CardContent>
        </Card>
    );
}

interface TaskStatCardsProps {
    /** Absent when the user has no human approvals to count. */
    readonly approvals?: StatValue;
    /** AIM lists approvals per environment, unlike the other tasks, which span them all. */
    readonly approvalsEnvironmentName?: string;
    readonly total: StatValue;
    readonly subscriptions: number | null;
    readonly reviews: number | null;
    readonly promotions: number | null;
}

export function TaskStatCards({ approvals, approvalsEnvironmentName, total, subscriptions, reviews, promotions }: TaskStatCardsProps) {
    return (
        <div className="flex flex-wrap gap-4">
            {approvals !== undefined && (
                <StatCard
                    Icon={ShieldCheckIcon}
                    title={approvalsEnvironmentName ? `Approvals in ${approvalsEnvironmentName}` : 'Approvals'}
                    total={approvals}
                />
            )}
            <StatCard Icon={InboxIcon} title="Pending tasks" total={total} />
            <StatCard Icon={KeyIcon} title="Subscriptions" total={subscriptions} />
            <StatCard Icon={EyeIcon} title="API reviews" total={reviews} />
            <StatCard Icon={CloudUploadIcon} title="Promotions" total={promotions} />
        </div>
    );
}
