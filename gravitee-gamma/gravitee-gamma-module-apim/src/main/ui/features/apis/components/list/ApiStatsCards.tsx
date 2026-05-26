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

import { useApiStats } from '../../hooks/useApiStats';
import { useEnvironmentTotalCalls } from '../../hooks/useEnvironmentTotalCalls';

const STAT_CARDS = [
    { key: 'total' as const, label: 'Total APIs' },
    { key: 'private' as const, label: 'Private' },
    { key: 'published' as const, label: 'Published' },
] as const;

function StatCard({ label, value, isLoading }: { label: string; value: number | null; isLoading: boolean }) {
    return (
        <Card style={{ flex: 1 }}>
            <CardContent className="pt-5 pb-4">
                <p className="text-sm font-medium text-muted-foreground">{label}</p>
                {value === null ? (
                    <Skeleton className="mt-1.5 h-7 w-10 rounded" />
                ) : (
                    <p className={`text-2xl font-semibold mt-0.5 transition-opacity duration-200${isLoading ? ' opacity-50' : ''}`}>
                        {value.toLocaleString()}
                    </p>
                )}
            </CardContent>
        </Card>
    );
}

export function ApiStatsCards({ query }: { query?: string }) {
    const stats = useApiStats(query);
    const calls = useEnvironmentTotalCalls();

    return (
        <div className="flex gap-4">
            {STAT_CARDS.map(({ key, label }) => (
                <StatCard key={key} label={label} value={stats[key]} isLoading={stats.isLoading} />
            ))}
            <StatCard label="Total Calls (24h)" value={calls.total} isLoading={calls.isLoading} />
        </div>
    );
}
