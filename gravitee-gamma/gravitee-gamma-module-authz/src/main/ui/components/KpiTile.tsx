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
import type { ComponentType, SVGProps } from 'react';

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
    readonly Icon?: ComponentType<SVGProps<SVGSVGElement>>;
    readonly iconClassName?: string;
}

export function KpiTile({ label, value, loading = false, tone = 'default', Icon, iconClassName }: KpiTileProps) {
    return (
        <Card className="flex flex-col gap-3 p-5" aria-label={label}>
            {Icon && (
                <div className={cn('flex size-10 items-center justify-center rounded-lg', iconClassName ?? 'bg-muted text-muted-foreground')}>
                    <Icon className="size-5" aria-hidden />
                </div>
            )}
            <div className="flex flex-col gap-0.5">
                {loading ? (
                    <Skeleton className="h-8 w-12" />
                ) : (
                    <div className={cn('text-3xl font-semibold leading-none', TONE_CLASS[tone])}>{value}</div>
                )}
                <p className="text-sm font-medium">{label}</p>
            </div>
        </Card>
    );
}
