/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Button, Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { ArrowRightIcon, CircleCheckIcon, ClockIcon, PlugIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';
import { Link } from 'react-router-dom';

import type { ApplicationOverviewData } from '../../hooks/useApplicationOverviewData';

interface SnapshotMetricCardProps {
    readonly helper: string;
    readonly icon: ComponentType<{ className?: string }>;
    readonly iconClassName: string;
    readonly isLoading: boolean;
    readonly label: string;
    readonly value: number;
}

function SnapshotMetricCard({ helper, icon: Icon, iconClassName, isLoading, label, value }: SnapshotMetricCardProps) {
    return (
        <Card className="flex-1">
            <CardContent className="pt-6">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="mb-2 text-sm text-muted-foreground">{label}</p>
                        {isLoading ? <Skeleton className="h-8 w-12" /> : <p className="text-2xl font-semibold tracking-tight">{value}</p>}
                        <p className="mt-2 text-xs text-muted-foreground">{helper}</p>
                    </div>
                    <Icon className={iconClassName} aria-hidden />
                </div>
            </CardContent>
        </Card>
    );
}

export function ApplicationOverviewSnapshot({ overviewData }: Readonly<{ overviewData: ApplicationOverviewData }>) {
    return (
        <section className="space-y-3" aria-labelledby="application-overview-snapshot-title">
            <div className="flex items-center justify-between gap-4">
                <h2 id="application-overview-snapshot-title" className="text-sm font-medium text-muted-foreground">
                    Application snapshot
                </h2>
                <Button asChild variant="link" size="sm" className="h-auto px-0 text-primary">
                    <Link to="../subscriptions">
                        More details
                        <ArrowRightIcon className="size-4" aria-hidden />
                    </Link>
                </Button>
            </div>

            <div className="flex gap-4">
                <SnapshotMetricCard
                    helper="Total API subscriptions"
                    icon={PlugIcon}
                    iconClassName="size-4 text-muted-foreground"
                    isLoading={overviewData.isLoadingSubscriptions}
                    label="Subscriptions"
                    value={overviewData.subscriptionCount}
                />
                <SnapshotMetricCard
                    helper="Consuming APIs"
                    icon={CircleCheckIcon}
                    iconClassName="size-4 text-success"
                    isLoading={overviewData.isLoadingActiveSubscriptions}
                    label="Active"
                    value={overviewData.activeSubscriptionCount}
                />
                <SnapshotMetricCard
                    helper="Awaiting approval"
                    icon={ClockIcon}
                    iconClassName="size-4 text-warning"
                    isLoading={overviewData.isLoadingPendingSubscriptions}
                    label="Pending"
                    value={overviewData.pendingSubscriptionCount}
                />
            </div>
        </section>
    );
}
