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
    DateRangePicker,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { DownloadIcon, XIcon } from '@gravitee/graphene-core/icons';
import type { DateRange } from 'react-day-picker';

import type {
    AuditDatePreset,
    AuditExportFormat,
    AuditGroupedRefs,
    AuditNamedRef,
    AuditReferenceType,
    AuditScope,
} from '../types/auditLog';

const ALL_EVENTS = '__all__';
const ALL_TYPES = '__all__';
const ALL_REFS = '__all__';

export const ORG_AUDIT_REFERENCE_TYPES: AuditReferenceType[] = ['ORGANIZATION', 'ENVIRONMENT', 'APPLICATION', 'API'];
export const ENV_AUDIT_REFERENCE_TYPES: AuditReferenceType[] = ['APPLICATION', 'API'];

const DATE_PRESETS: Array<{ value: AuditDatePreset; label: string }> = [
    { value: '', label: 'Any time' },
    { value: '24h', label: 'Last 24 hours' },
    { value: '7d', label: 'Last 7 days' },
    { value: '30d', label: 'Last 30 days' },
    { value: '90d', label: 'Last 90 days' },
    { value: 'custom', label: 'Custom' },
];

export interface AuditLogsFiltersProps {
    readonly scope: AuditScope;
    readonly eventTypes: readonly string[];
    readonly event: string;
    readonly onEventChange: (event: string) => void;
    readonly referenceType: AuditReferenceType | '';
    readonly onReferenceTypeChange: (type: AuditReferenceType | '') => void;
    readonly environments?: readonly AuditNamedRef[];
    readonly environmentId: string;
    readonly onEnvironmentIdChange: (id: string) => void;
    readonly applications: readonly AuditNamedRef[] | readonly AuditGroupedRefs[];
    readonly applicationId: string;
    readonly onApplicationIdChange: (id: string) => void;
    readonly apis: readonly AuditNamedRef[] | readonly AuditGroupedRefs[];
    readonly apiId: string;
    readonly onApiIdChange: (id: string) => void;
    readonly datePreset: AuditDatePreset;
    readonly onDatePresetChange: (preset: AuditDatePreset) => void;
    readonly customRange: DateRange | undefined;
    readonly onCustomRangeChange: (range: DateRange | undefined) => void;
    readonly onReset: () => void;
    readonly onExport: () => void;
}

function isGrouped(items: readonly AuditNamedRef[] | readonly AuditGroupedRefs[]): items is readonly AuditGroupedRefs[] {
    return items.length > 0 && 'group' in items[0];
}

function RefOptions({ items }: { items: readonly AuditNamedRef[] | readonly AuditGroupedRefs[] }) {
    if (isGrouped(items)) {
        return (
            <>
                {items.flatMap(group =>
                    group.items.map(item => (
                        <SelectItem key={`${group.group}:${item.id}`} value={item.id}>
                            {group.group} / {item.name}
                        </SelectItem>
                    )),
                )}
            </>
        );
    }
    return (
        <>
            {items.map(item => (
                <SelectItem key={item.id} value={item.id}>
                    {item.name}
                </SelectItem>
            ))}
        </>
    );
}

export function AuditLogsFilters({
    scope,
    eventTypes,
    event,
    onEventChange,
    referenceType,
    onReferenceTypeChange,
    environments = [],
    environmentId,
    onEnvironmentIdChange,
    applications,
    applicationId,
    onApplicationIdChange,
    apis,
    apiId,
    onApiIdChange,
    datePreset,
    onDatePresetChange,
    customRange,
    onCustomRangeChange,
    onReset,
    onExport,
}: AuditLogsFiltersProps) {
    const types = scope === 'organization' ? ORG_AUDIT_REFERENCE_TYPES : ENV_AUDIT_REFERENCE_TYPES;
    const hasFilters = Boolean(event || referenceType || datePreset || customRange?.from || customRange?.to);

    return (
        <div className="flex flex-wrap items-center gap-2">
            <Select value={event || ALL_EVENTS} onValueChange={value => onEventChange(value === ALL_EVENTS ? '' : value)}>
                <SelectTrigger className="w-52" aria-label="Filter by event type">
                    <SelectValue placeholder="All events" />
                </SelectTrigger>
                <SelectContent position="popper" className="max-h-72 overflow-y-auto">
                    <SelectItem value={ALL_EVENTS}>All events</SelectItem>
                    {eventTypes.map(name => (
                        <SelectItem key={name} value={name}>
                            {name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>

            <Select
                value={referenceType || ALL_TYPES}
                onValueChange={value => onReferenceTypeChange(value === ALL_TYPES ? '' : (value as AuditReferenceType))}
            >
                <SelectTrigger className="w-44" aria-label="Filter by type">
                    <SelectValue placeholder="All types" />
                </SelectTrigger>
                <SelectContent position="popper">
                    <SelectItem value={ALL_TYPES}>All types</SelectItem>
                    {types.map(type => (
                        <SelectItem key={type} value={type}>
                            {type}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>

            {scope === 'organization' && referenceType === 'ENVIRONMENT' ? (
                <Select value={environmentId || ALL_REFS} onValueChange={value => onEnvironmentIdChange(value === ALL_REFS ? '' : value)}>
                    <SelectTrigger className="w-52" aria-label="Filter by environment">
                        <SelectValue placeholder="All environments" />
                    </SelectTrigger>
                    <SelectContent position="popper" className="max-h-72 overflow-y-auto">
                        <SelectItem value={ALL_REFS}>All environments</SelectItem>
                        {environments.map(item => (
                            <SelectItem key={item.id} value={item.id}>
                                {item.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            ) : null}

            {referenceType === 'APPLICATION' ? (
                <Select value={applicationId || ALL_REFS} onValueChange={value => onApplicationIdChange(value === ALL_REFS ? '' : value)}>
                    <SelectTrigger className="w-52" aria-label="Filter by application">
                        <SelectValue placeholder="All applications" />
                    </SelectTrigger>
                    <SelectContent position="popper" className="max-h-72 overflow-y-auto">
                        <SelectItem value={ALL_REFS}>All applications</SelectItem>
                        <RefOptions items={applications} />
                    </SelectContent>
                </Select>
            ) : null}

            {referenceType === 'API' ? (
                <Select value={apiId || ALL_REFS} onValueChange={value => onApiIdChange(value === ALL_REFS ? '' : value)}>
                    <SelectTrigger className="w-52" aria-label="Filter by API">
                        <SelectValue placeholder="All APIs" />
                    </SelectTrigger>
                    <SelectContent position="popper" className="max-h-72 overflow-y-auto">
                        <SelectItem value={ALL_REFS}>All APIs</SelectItem>
                        <RefOptions items={apis} />
                    </SelectContent>
                </Select>
            ) : null}

            <Select
                value={datePreset || ALL_EVENTS}
                onValueChange={value => onDatePresetChange((value === ALL_EVENTS ? '' : value) as AuditDatePreset)}
            >
                <SelectTrigger className="w-44" aria-label="Filter by time period">
                    <SelectValue placeholder="Any time" />
                </SelectTrigger>
                <SelectContent position="popper">
                    {DATE_PRESETS.map(preset => (
                        <SelectItem key={preset.value || ALL_EVENTS} value={preset.value || ALL_EVENTS}>
                            {preset.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>

            <DateRangePicker
                value={customRange}
                onChange={onCustomRangeChange}
                placeholder="Date range"
                numberOfMonths={2}
                aria-label="Filter by date range"
                className="w-64"
            />

            {hasFilters ? (
                <Button type="button" variant="ghost" size="sm" onClick={onReset} className="gap-1">
                    <XIcon className="size-3.5" aria-hidden />
                    Reset
                </Button>
            ) : null}

            <Button type="button" variant="outline" size="sm" className="ml-auto gap-1" onClick={onExport}>
                <DownloadIcon className="size-3.5" aria-hidden />
                Export
            </Button>
        </div>
    );
}

export function AuditExportDialog({
    open,
    exporting,
    onOpenChange,
    onConfirm,
}: Readonly<{
    open: boolean;
    exporting: boolean;
    onOpenChange: (open: boolean) => void;
    onConfirm: (format: AuditExportFormat) => void;
}>) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-full max-w-md sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Export audit logs</DialogTitle>
                    <DialogDescription>Download the current filtered results as CSV or JSON.</DialogDescription>
                </DialogHeader>
                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" disabled={exporting} onClick={() => onConfirm('csv')}>
                        CSV
                    </Button>
                    <Button type="button" disabled={exporting} onClick={() => onConfirm('json')}>
                        JSON
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
