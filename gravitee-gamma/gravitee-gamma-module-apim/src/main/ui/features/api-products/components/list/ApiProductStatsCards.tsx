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

interface StatsCardProps {
    label: string;
    value: number | null;
}

function StatsCard({ label, value }: StatsCardProps) {
    return (
        <Card className="flex-1">
            <CardContent className="pt-5 pb-4">
                <p className="text-sm font-medium text-muted-foreground">{label}</p>
                {value === null ? (
                    <Skeleton className="mt-1.5 h-7 w-10 rounded" />
                ) : (
                    <p className="text-2xl font-semibold mt-0.5">{value}</p>
                )}
            </CardContent>
        </Card>
    );
}

interface ApiProductStatsCardsProps {
    totalProducts: number | null;
}

export function ApiProductStatsCards({ totalProducts }: ApiProductStatsCardsProps) {
    return (
        <div className="flex gap-4">
            <StatsCard label="Total Products" value={totalProducts} />
            <StatsCard label="Published" value={0} />
            <StatsCard label="Subscribers" value={0} />
        </div>
    );
}
