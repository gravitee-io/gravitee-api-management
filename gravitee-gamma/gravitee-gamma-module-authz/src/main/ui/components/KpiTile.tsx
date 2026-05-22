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
/**
 * Single KPI tile: large numeric value on top, small label below, optional
 * tone tint for affirmative / muted variants. Hoist candidate for
 * `@gravitee/graphene-core` — every Gamma module needs the same dashboard
 * KPI strip.
 */
import { Card } from '@gravitee/graphene-core';

export interface KpiTileProps {
    readonly label: string;
    readonly value: number | string;
    readonly loading?: boolean;
    readonly tone?: 'default' | 'success' | 'muted';
}

export function KpiTile({ label, value, loading = false, tone = 'default' }: KpiTileProps) {
    const valueClass =
        tone === 'success'
            ? 'text-2xl text-success'
            : tone === 'muted'
              ? 'text-2xl text-muted-foreground'
              : 'text-2xl';
    return (
        <Card role="listitem" className="p-4" aria-label={label}>
            <div className={valueClass} aria-live="polite">
                {loading ? '—' : value}
            </div>
            <p className="text-xs text-muted-foreground">{label}</p>
        </Card>
    );
}
