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
import {
    Button,
    Card,
    CardContent,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';

import { isSubscriptionFiltersDirty } from '../../features/apis/hooks/useSubscriptions';
import type { Plan, SubscriptionFilters, SubscriptionStatus } from '../../features/apis/types/subscription';

const ALL_STATUSES: SubscriptionStatus[] = ['PENDING', 'ACCEPTED', 'REJECTED', 'CLOSED', 'PAUSED', 'RESUMED'];

interface ConsumersFilterBarProps {
    filters: SubscriptionFilters;
    plans: Plan[];
    onChange: (filters: SubscriptionFilters) => void;
}

export function ConsumersFilterBar({ filters, plans, onChange }: Readonly<ConsumersFilterBarProps>) {
    const isDirty = isSubscriptionFiltersDirty(filters);

    const handleStatus = useCallback(
        (value: string) => {
            if (value === '__all__') {
                onChange({ ...filters, statuses: [] });
            } else {
                onChange({ ...filters, statuses: [value as SubscriptionStatus] });
            }
        },
        [filters, onChange],
    );

    const handlePlan = useCallback(
        (value: string) => {
            onChange({ ...filters, planIds: value === '__all__' ? [] : [value] });
        },
        [filters, onChange],
    );

    const handleApiKey = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onChange({ ...filters, apiKey: e.target.value });
        },
        [filters, onChange],
    );

    const handleReset = useCallback(() => {
        onChange({ statuses: [], planIds: [], applicationIds: [], apiKey: '' });
    }, [onChange]);

    const currentStatus = filters.statuses.length === 1 ? filters.statuses[0] : '__all__';
    const currentPlan = filters.planIds.length === 1 ? filters.planIds[0] : '__all__';

    return (
        <Card>
            <CardContent className="pt-4 pb-4">
                <div className="flex flex-wrap gap-4">
                    <div className="flex-1 space-y-1.5" style={{ minWidth: '140px' }}>
                        <Label className="text-xs">Status</Label>
                        <Select value={currentStatus} onValueChange={handleStatus}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="All statuses" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="__all__">All statuses</SelectItem>
                                {ALL_STATUSES.map(s => (
                                    <SelectItem key={s} value={s}>
                                        {s.charAt(0) + s.slice(1).toLowerCase()}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="flex-1 space-y-1.5" style={{ minWidth: '140px' }}>
                        <Label className="text-xs">Plan</Label>
                        <Select value={currentPlan} onValueChange={handlePlan}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="All plans" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="__all__">All plans</SelectItem>
                                {plans.map(p => (
                                    <SelectItem key={p.id} value={p.id}>
                                        {p.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="flex-1 space-y-1.5" style={{ minWidth: '160px' }}>
                        <Label className="text-xs">API Key</Label>
                        <Input className="w-full" placeholder="Search by key…" value={filters.apiKey} onChange={handleApiKey} />
                    </div>

                    {isDirty && (
                        <div className="flex items-end">
                            <Button type="button" variant="ghost" size="sm" className="gap-1.5 text-muted-foreground" onClick={handleReset}>
                                <XIcon className="size-3.5" aria-hidden />
                                Reset
                            </Button>
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
