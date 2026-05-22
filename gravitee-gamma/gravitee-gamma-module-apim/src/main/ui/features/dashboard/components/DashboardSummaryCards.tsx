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
import { ArchiveIcon, RadioIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

interface StatCardProps {
    Icon: LucideIcon;
    title: string;
    total: number | null;
}

function StatCard({ Icon, title, total }: StatCardProps) {
    return (
        <Card>
            <CardContent className="pt-5 pb-5">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <div className="rounded-lg bg-primary/10 p-2">
                            <Icon className="size-5 text-primary" aria-hidden />
                        </div>
                        <p className="text-sm font-medium text-muted-foreground">{title}</p>
                    </div>
                    {total === null ? (
                        <Skeleton className="rounded" style={{ height: '2rem', width: '2.5rem' }} />
                    ) : (
                        <p className="text-2xl font-semibold">{total}</p>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}

interface DashboardSummaryCardsProps {
    totalApis: number | null;
    totalProducts: number | null;
}

export function DashboardSummaryCards({ totalApis, totalProducts }: DashboardSummaryCardsProps) {
    return (
        <div className="grid grid-cols-2 gap-4">
            <StatCard Icon={RadioIcon} title="Total APIs" total={totalApis} />
            <StatCard Icon={ArchiveIcon} title="Total API Products" total={totalProducts} />
        </div>
    );
}
