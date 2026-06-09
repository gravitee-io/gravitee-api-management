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
import { Button, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';
import { RefreshCwIcon } from '@gravitee/graphene-core/icons';

import { TIMEFRAMES, type Timeframe } from '../../../../utils/healthTimeframe';

interface HealthCheckFiltersProps {
    timeframe: Timeframe;
    onTimeframeChange: (timeframe: Timeframe) => void;
    onRefresh: () => void;
}

export function HealthCheckFilters({ timeframe, onTimeframeChange, onRefresh }: Readonly<HealthCheckFiltersProps>) {
    return (
        <div className="flex flex-wrap items-end gap-4">
            <div className="flex flex-col gap-1.5">
                <Label htmlFor="health-check-timeframe">Timeframe</Label>
                <Select value={timeframe} onValueChange={value => onTimeframeChange(value as Timeframe)}>
                    <SelectTrigger id="health-check-timeframe" className="w-44">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {TIMEFRAMES.map(option => (
                            <SelectItem key={option.id} value={option.id}>
                                {option.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>
            <Button type="button" variant="outline" size="sm" onClick={onRefresh}>
                <RefreshCwIcon className="size-4" aria-hidden="true" />
                Refresh data
            </Button>
        </div>
    );
}
