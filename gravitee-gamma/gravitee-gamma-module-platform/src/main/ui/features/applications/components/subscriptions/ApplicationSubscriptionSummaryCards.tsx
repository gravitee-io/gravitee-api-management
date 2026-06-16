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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';

interface StatCardProps {
    value: number | string;
    label: string;
    isLoading: boolean;
}

function StatCard({ value, label, isLoading }: Readonly<StatCardProps>) {
    return (
        <Card>
            <CardContent className="pt-5 pb-5">
                {isLoading ? (
                    <div className="space-y-2">
                        <Skeleton className="h-7 w-10 rounded" />
                        <Skeleton className="h-4 w-24 rounded" />
                    </div>
                ) : (
                    <div className="space-y-1">
                        <p className="text-2xl font-bold">{value}</p>
                        <p className="text-sm text-muted-foreground">{label}</p>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}

interface ApplicationSubscriptionSummaryCardsProps {
    totalCount: number;
    acceptedCount: number;
    pendingCount: number;
    isLoading: boolean;
}

/** Total / Accepted / Pending subscription cards, mirroring the APIM Consumers page summary. */
export function ApplicationSubscriptionSummaryCards({
    totalCount,
    acceptedCount,
    pendingCount,
    isLoading,
}: Readonly<ApplicationSubscriptionSummaryCardsProps>) {
    return (
        <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
            <StatCard value={totalCount} label="Total consumers" isLoading={isLoading} />
            <StatCard value={acceptedCount} label="Accepted" isLoading={isLoading} />
            <StatCard value={pendingCount} label="Pending" isLoading={isLoading} />
        </div>
    );
}
