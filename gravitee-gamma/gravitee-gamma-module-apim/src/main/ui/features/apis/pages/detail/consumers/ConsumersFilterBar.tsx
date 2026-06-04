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
import { Button, Card, CardContent, Input, Label } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo } from 'react';

import { MultiSelectFilter, type MultiSelectFilterOption } from '../../../../../shared/components';
import { isSubscriptionFiltersDirty } from '../../../hooks/useSubscriptions';
import type { Plan, SubscriptionFilters, SubscriptionStatus } from '../../../types/subscription';

const ALL_STATUSES: SubscriptionStatus[] = ['PENDING', 'ACCEPTED', 'REJECTED', 'CLOSED', 'PAUSED', 'RESUMED'];

const STATUS_OPTIONS: MultiSelectFilterOption[] = ALL_STATUSES.map(s => ({
    value: s,
    label: s.charAt(0) + s.slice(1).toLowerCase(),
}));

interface ConsumersFilterBarProps {
    filters: SubscriptionFilters;
    plans: Plan[];
    onChange: (filters: SubscriptionFilters) => void;
}

export function ConsumersFilterBar({ filters, plans, onChange }: Readonly<ConsumersFilterBarProps>) {
    const isDirty = isSubscriptionFiltersDirty(filters);

    const planOptions = useMemo<MultiSelectFilterOption[]>(() => plans.map(p => ({ value: p.id, label: p.name })), [plans]);

    const handleStatuses = useCallback(
        (values: string[]) => {
            onChange({ ...filters, statuses: values as SubscriptionStatus[] });
        },
        [filters, onChange],
    );

    const handlePlans = useCallback(
        (values: string[]) => {
            onChange({ ...filters, planIds: values });
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

    return (
        <Card>
            <CardContent className="pt-4 pb-4">
                <div className="flex flex-wrap gap-4">
                    <div className="flex-1 space-y-1.5" style={{ minWidth: '140px' }}>
                        <Label className="text-xs">Status</Label>
                        <MultiSelectFilter
                            placeholder="All statuses"
                            ariaLabel="Filter by status"
                            options={STATUS_OPTIONS}
                            selectedValues={filters.statuses}
                            onSelectedValuesChange={handleStatuses}
                        />
                    </div>

                    <div className="flex-1 space-y-1.5" style={{ minWidth: '140px' }}>
                        <Label className="text-xs">Plan</Label>
                        <MultiSelectFilter
                            placeholder="All plans"
                            ariaLabel="Filter by plan"
                            options={planOptions}
                            selectedValues={filters.planIds}
                            onSelectedValuesChange={handlePlans}
                            emptyMessage="No plans available"
                        />
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
