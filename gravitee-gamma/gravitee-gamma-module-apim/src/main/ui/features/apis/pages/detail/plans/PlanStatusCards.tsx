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
import { Card, CardContent, Skeleton, cn } from '@gravitee/graphene-core';
import { CircleCheckIcon, ClockIcon, LockIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import type { PlanStatus } from '../../../types/plan';

const STATUS_CONFIG: Record<PlanStatus, { color: string; icon: LucideIcon; label: string }> = {
    STAGING: { color: 'var(--color-warning)', icon: ClockIcon, label: 'Staging' },
    PUBLISHED: { color: 'var(--color-success)', icon: CircleCheckIcon, label: 'Published' },
    DEPRECATED: { color: 'var(--color-destructive)', icon: TriangleAlertIcon, label: 'Deprecated' },
    CLOSED: { color: 'var(--color-destructive)', icon: LockIcon, label: 'Closed' },
};

interface StatusCardProps {
    status: PlanStatus;
    count: number;
    isLoading: boolean;
    selected: boolean;
    onClick: () => void;
}

function StatusCard({ status, count, isLoading, selected, onClick }: Readonly<StatusCardProps>) {
    const { color, icon: Icon, label } = STATUS_CONFIG[status];
    return (
        <button type="button" className="flex-1 text-left" onClick={onClick}>
            <Card className={cn('cursor-pointer transition-all h-full', selected && 'ring-2 ring-primary')}>
                <CardContent className="px-4 py-3">
                    {isLoading ? (
                        <div className="flex items-center gap-3">
                            <Skeleton className="size-9 rounded-lg shrink-0" />
                            <div className="space-y-1.5 flex-1">
                                <Skeleton className="h-6 w-8 rounded" />
                                <Skeleton className="h-3 w-16 rounded" />
                            </div>
                        </div>
                    ) : (
                        <div className="flex items-center gap-3">
                            <div
                                className="rounded-lg p-2 shrink-0"
                                style={{ backgroundColor: `color-mix(in oklab, ${color} 12%, transparent)` }}
                            >
                                <Icon className="size-5" style={{ color }} aria-hidden />
                            </div>
                            <div>
                                <p className="text-xl font-bold leading-tight" style={{ color }}>
                                    {count}
                                </p>
                                <p className="text-xs text-muted-foreground">{label}</p>
                            </div>
                        </div>
                    )}
                </CardContent>
            </Card>
        </button>
    );
}

interface PlanStatusCardsProps {
    staging: number;
    published: number;
    deprecated: number;
    closed: number;
    isLoading: boolean;
    selectedStatus: PlanStatus | null;
    onStatusSelect: (status: PlanStatus) => void;
}

export function PlanStatusCards({
    staging,
    published,
    deprecated,
    closed,
    isLoading,
    selectedStatus,
    onStatusSelect,
}: Readonly<PlanStatusCardsProps>) {
    return (
        <div className="flex gap-4">
            <StatusCard
                status="STAGING"
                count={staging}
                isLoading={isLoading}
                selected={selectedStatus === 'STAGING'}
                onClick={() => onStatusSelect('STAGING')}
            />
            <StatusCard
                status="PUBLISHED"
                count={published}
                isLoading={isLoading}
                selected={selectedStatus === 'PUBLISHED'}
                onClick={() => onStatusSelect('PUBLISHED')}
            />
            <StatusCard
                status="DEPRECATED"
                count={deprecated}
                isLoading={isLoading}
                selected={selectedStatus === 'DEPRECATED'}
                onClick={() => onStatusSelect('DEPRECATED')}
            />
            <StatusCard
                status="CLOSED"
                count={closed}
                isLoading={isLoading}
                selected={selectedStatus === 'CLOSED'}
                onClick={() => onStatusSelect('CLOSED')}
            />
        </div>
    );
}
