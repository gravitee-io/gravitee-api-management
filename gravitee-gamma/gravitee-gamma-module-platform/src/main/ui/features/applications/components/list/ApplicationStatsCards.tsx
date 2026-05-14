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
import { Card, CardContent, cn, Skeleton } from '@gravitee/graphene-core';
import { useEffect, useRef } from 'react';

import type { ApplicationStats } from '../../hooks/useApplicationStats';

const STAT_CARDS = [
    { key: 'active' as const, label: 'Active applications', hint: 'in this environment' },
    { key: 'archived' as const, label: 'Archived applications', hint: 'in this environment' },
] as const;

function StatCard({ label, hint, value, isLoading }: { label: string; hint: string; value: number | null; isLoading: boolean }) {
    const lastKnown = useRef<number | null>(null);

    useEffect(() => {
        if (value !== null) {
            lastKnown.current = value;
        }
    }, [value]);

    const displayValue = value ?? lastKnown.current;

    return (
        <Card style={{ flex: 1 }}>
            <CardContent className="pt-5 pb-4">
                <p className="text-sm font-medium text-muted-foreground">{label}</p>
                {displayValue === null ? (
                    <Skeleton className="mt-1.5 h-7 w-10 rounded" />
                ) : (
                    <p
                        className={cn('text-2xl font-semibold mt-0.5 transition-opacity duration-200', {
                            'opacity-50': isLoading,
                        })}
                    >
                        {displayValue}
                    </p>
                )}
                <p className="text-xs text-muted-foreground mt-1">{hint}</p>
            </CardContent>
        </Card>
    );
}

export function ApplicationStatsCards({ stats }: { readonly stats: ApplicationStats }) {
    return (
        <div className="flex gap-4">
            {STAT_CARDS.map(({ key, label, hint }) => (
                <StatCard
                    key={key}
                    label={label}
                    hint={hint}
                    value={stats[key]}
                    isLoading={key === 'active' ? stats.isLoadingActive : stats.isLoadingArchived}
                />
            ))}
        </div>
    );
}
