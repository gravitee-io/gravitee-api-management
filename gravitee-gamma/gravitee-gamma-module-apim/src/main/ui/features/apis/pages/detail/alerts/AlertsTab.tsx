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
    CardDescription,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import type { Dispatch, SetStateAction } from 'react';

import { AggregationConditionForm } from './AggregationConditionForm';
import { FilterRow } from './FilterRow';
import { MissingDataConditionForm } from './MissingDataConditionForm';
import { RateConditionForm } from './RateConditionForm';
import { SimpleConditionForm } from './SimpleConditionForm';
import { ALERT_RULES, API_METRICS, type AlertMetricDefinition, type AlertRuleDefinition } from '../../../constants/alertConstants';
import type { AlertFormCondition, AlertFormTimeframe, AlertRuleId, AlertSeverity } from '../../../types/api';

const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export interface AlertsTabProps {
    name: string;
    setName: Dispatch<SetStateAction<string>>;
    description: string;
    setDescription: Dispatch<SetStateAction<string>>;
    severity: AlertSeverity;
    setSeverity: Dispatch<SetStateAction<AlertSeverity>>;
    enabled: boolean;
    setEnabled: Dispatch<SetStateAction<boolean>>;
    ruleId: AlertRuleId;
    handleRuleChange: (newRuleId: AlertRuleId) => void;
    isUpdate: boolean;
    canEdit: boolean;
    errors: Record<string, string>;
    setErrors: Dispatch<SetStateAction<Record<string, string>>>;
    markDirty: () => void;
    timeframes: AlertFormTimeframe[];
    addTimeframe: () => void;
    removeTimeframe: (index: number) => void;
    toggleTimeframeDay: (index: number, dayNum: number) => void;
    updateTimeframeHour: (index: number, field: 'startHour' | 'endHour', value: number) => void;
    conditions: AlertFormCondition[];
    updateCondition: (index: number, c: AlertFormCondition) => void;
    metricsForRule: AlertMetricDefinition[];
    filters: AlertFormCondition[];
    addFilter: () => void;
    updateFilter: (index: number, f: AlertFormCondition) => void;
    removeFilter: (index: number) => void;
    selectedRule: AlertRuleDefinition | undefined;
}

export function AlertsTab({
    name,
    setName,
    description,
    setDescription,
    severity,
    setSeverity,
    enabled,
    setEnabled,
    ruleId,
    handleRuleChange,
    isUpdate,
    canEdit,
    errors,
    setErrors,
    markDirty,
    timeframes,
    addTimeframe,
    removeTimeframe,
    toggleTimeframeDay,
    updateTimeframeHour,
    conditions,
    updateCondition,
    metricsForRule,
    filters,
    addFilter,
    updateFilter,
    removeFilter,
    selectedRule,
}: AlertsTabProps) {
    return (
        <div className="mt-6 space-y-6">
            {/* General */}
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">General</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="flex items-end gap-4">
                        <div className="flex-1 space-y-1.5">
                            <Label htmlFor="alert-name" className="text-xs">
                                Name <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="alert-name"
                                placeholder="Alert name"
                                value={name}
                                disabled={!canEdit}
                                className={errors.name ? 'border-destructive' : ''}
                                onChange={e => {
                                    setName(e.target.value);
                                    markDirty();
                                    if (errors.name) setErrors(p => ({ ...p, name: '' }));
                                }}
                            />
                            {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
                        </div>
                        <div className="flex items-center gap-2 pb-1">
                            <Label htmlFor="alert-enabled" className="text-xs text-muted-foreground">
                                Enable alert
                            </Label>
                            <Switch
                                id="alert-enabled"
                                checked={enabled}
                                disabled={!canEdit}
                                onCheckedChange={v => {
                                    setEnabled(v);
                                    markDirty();
                                }}
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1.5">
                            <Label className="text-xs">
                                Rule <span className="text-destructive">*</span>
                            </Label>
                            <Select
                                value={ruleId}
                                disabled={isUpdate || !canEdit}
                                onValueChange={val => handleRuleChange(val as AlertRuleId)}
                            >
                                <SelectTrigger className={isUpdate ? 'opacity-60' : ''}>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {ALERT_RULES.map(rule => (
                                        <SelectItem key={rule.id} value={rule.id}>
                                            {rule.description}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-xs">
                                Severity <span className="text-destructive">*</span>
                            </Label>
                            <Select
                                value={severity}
                                disabled={!canEdit}
                                onValueChange={val => {
                                    setSeverity(val as AlertSeverity);
                                    markDirty();
                                }}
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="INFO">info</SelectItem>
                                    <SelectItem value="WARNING">warning</SelectItem>
                                    <SelectItem value="CRITICAL">critical</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <div className="space-y-1.5">
                        <Label htmlFor="alert-desc" className="text-xs">
                            Description
                        </Label>
                        <Textarea
                            id="alert-desc"
                            placeholder="Description"
                            value={description}
                            disabled={!canEdit}
                            rows={3}
                            maxLength={256}
                            className="resize-none"
                            onChange={e => {
                                setDescription(e.target.value);
                                markDirty();
                            }}
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Timeframes */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <div>
                            <CardTitle className="text-base">Timeframes</CardTitle>
                            <CardDescription>Choose timeframe when notifications should be sent.</CardDescription>
                        </div>
                        {canEdit && (
                            <Button variant="outline" size="sm" onClick={addTimeframe}>
                                Add timeframe
                            </Button>
                        )}
                    </div>
                </CardHeader>
                {timeframes.length > 0 && (
                    <CardContent className="space-y-3 pt-0">
                        {timeframes.map((tf, idx) => (
                            <Card key={idx}>
                                <CardContent className="px-4 py-3 space-y-3">
                                    <div className="flex items-center justify-between">
                                        <span className="text-sm font-medium">Configure timeframe</span>
                                        {canEdit && (
                                            <Button variant="ghost" size="icon" className="size-7" onClick={() => removeTimeframe(idx)}>
                                                <XIcon className="size-3.5 text-muted-foreground" />
                                            </Button>
                                        )}
                                    </div>
                                    <div className="flex flex-wrap gap-1.5">
                                        {DAY_LABELS.map((day, dayIdx) => {
                                            const dayNum = dayIdx + 1;
                                            const isSelected = tf.days.includes(dayNum);
                                            return (
                                                <button
                                                    key={day}
                                                    type="button"
                                                    disabled={!canEdit}
                                                    className={`rounded-md px-2.5 py-1 text-xs font-medium border transition-colors ${
                                                        isSelected
                                                            ? 'bg-primary text-primary-foreground border-primary'
                                                            : 'bg-background text-muted-foreground border-border hover:bg-accent'
                                                    }`}
                                                    onClick={() => toggleTimeframeDay(idx, dayNum)}
                                                >
                                                    {day}
                                                </button>
                                            );
                                        })}
                                    </div>
                                    <div className="grid grid-cols-2 gap-3">
                                        <div className="space-y-1.5">
                                            <Label className="text-xs">Start hour</Label>
                                            <Input
                                                type="number"
                                                min={0}
                                                max={23}
                                                disabled={!canEdit}
                                                value={tf.startHour}
                                                onChange={e => updateTimeframeHour(idx, 'startHour', Number(e.target.value))}
                                            />
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label className="text-xs">End hour</Label>
                                            <Input
                                                type="number"
                                                min={0}
                                                max={23}
                                                disabled={!canEdit}
                                                value={tf.endHour}
                                                onChange={e => updateTimeframeHour(idx, 'endHour', Number(e.target.value))}
                                            />
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </CardContent>
                )}
            </Card>

            {/* Conditions */}
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Conditions</CardTitle>
                    <CardDescription>Field metrics and condition for the rule</CardDescription>
                </CardHeader>
                <CardContent>
                    {ruleId === 'REQUEST@METRICS_SIMPLE_CONDITION' && conditions[0] && (
                        <SimpleConditionForm condition={conditions[0]} metrics={metricsForRule} onChange={c => updateCondition(0, c)} />
                    )}
                    {ruleId === 'REQUEST@MISSING_DATA' && conditions[0] && (
                        <MissingDataConditionForm condition={conditions[0]} onChange={c => updateCondition(0, c)} />
                    )}
                    {ruleId === 'REQUEST@METRICS_AGGREGATION' && conditions[0] && (
                        <AggregationConditionForm
                            condition={conditions[0]}
                            metrics={metricsForRule}
                            onChange={c => updateCondition(0, c)}
                        />
                    )}
                    {ruleId === 'REQUEST@METRICS_RATE' && conditions[0] && (
                        <RateConditionForm condition={conditions[0]} metrics={metricsForRule} onChange={c => updateCondition(0, c)} />
                    )}
                    {ruleId === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED' && (
                        <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
                            This alert triggers automatically when an endpoint&apos;s health check status transitions between healthy and
                            unhealthy states. No additional condition configuration is required.
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Filters */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <div>
                            <CardTitle className="text-base">Filters</CardTitle>
                            <CardDescription>Filters to apply condition only on a subset of events</CardDescription>
                        </div>
                        {selectedRule && canEdit && (
                            <Button variant="outline" size="sm" onClick={addFilter}>
                                Add filter
                            </Button>
                        )}
                    </div>
                </CardHeader>
                {!selectedRule ? (
                    <CardContent className="pt-0">
                        <div className="rounded-lg border bg-muted/30 p-3 text-sm text-muted-foreground">
                            Select a rule before setting the filters.
                        </div>
                    </CardContent>
                ) : filters.length > 0 ? (
                    <CardContent className="space-y-3 pt-0">
                        {filters.map((f, idx) => (
                            <FilterRow
                                key={idx}
                                filter={f}
                                index={idx}
                                metrics={API_METRICS}
                                onChange={updateFilter}
                                onRemove={removeFilter}
                            />
                        ))}
                    </CardContent>
                ) : null}
            </Card>
        </div>
    );
}
