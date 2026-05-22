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
import { Card, Skeleton, cn } from '@gravitee/graphene-core';

const TONE_CLASS = {
    default: '',
    success: 'text-success',
    muted: 'text-muted-foreground',
} as const;

export interface KpiTileProps {
    readonly label: string;
    readonly value: number | string;
    readonly loading?: boolean;
    readonly tone?: keyof typeof TONE_CLASS;
}

export function KpiTile({ label, value, loading = false, tone = 'default' }: KpiTileProps) {
    return (
        <Card className="p-4" aria-label={label}>
            {loading ? <Skeleton className="h-8 w-12" /> : <div className={cn('text-2xl', TONE_CLASS[tone])}>{value}</div>}
            <p className="text-xs text-muted-foreground">{label}</p>
        </Card>
    );
}
