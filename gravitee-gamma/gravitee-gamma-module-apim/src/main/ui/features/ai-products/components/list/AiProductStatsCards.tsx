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

function StatCard({ label, sub, value }: { label: string; sub: string; value: number | null }) {
    return (
        <Card style={{ flex: 1 }}>
            <CardContent className="pt-5 pb-4">
                <p className="text-sm font-medium text-muted-foreground">{label}</p>
                {value === null ? (
                    <Skeleton className="mt-1.5 h-7 w-10 rounded" />
                ) : (
                    <p className="text-2xl font-semibold mt-0.5">{value.toLocaleString()}</p>
                )}
                <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>
            </CardContent>
        </Card>
    );
}

interface AiProductStatsCardsProps {
    totalProducts: number | null;
    deployedProducts: number | null;
    subscribers: number | null;
}

export function AiProductStatsCards({ totalProducts, deployedProducts, subscribers }: AiProductStatsCardsProps) {
    return (
        <div className="flex gap-4">
            <StatCard label="Total products" sub="In this environment" value={totalProducts} />
            <StatCard label="Deployed" sub="Live on the gateway" value={deployedProducts} />
            <StatCard label="Subscribers" sub="Across all products" value={subscribers} />
        </div>
    );
}
