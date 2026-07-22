/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { LayoutGridIcon, ListIcon } from '@gravitee/graphene-core/icons';

import type { PortalPublishStatus } from '../utils/portal-display';

export type PortalsViewMode = 'cards' | 'table';
export type PortalsStatusFilter = 'all' | PortalPublishStatus;

interface PortalsFilterBarProps {
    readonly nameFilter: string;
    readonly onNameFilterChange: (value: string) => void;
    readonly statusFilter: PortalsStatusFilter;
    readonly onStatusFilterChange: (value: PortalsStatusFilter) => void;
    readonly viewMode: PortalsViewMode;
    readonly onViewModeChange: (value: PortalsViewMode) => void;
}

export function PortalsFilterBar({
    nameFilter,
    onNameFilterChange,
    statusFilter,
    onStatusFilterChange,
    viewMode,
    onViewModeChange,
}: PortalsFilterBarProps) {
    return (
        <div className="flex flex-wrap items-center gap-3">
            <Input
                value={nameFilter}
                onChange={event => onNameFilterChange(event.target.value)}
                placeholder="Filter portals..."
                aria-label="Filter portals by name"
                className="h-9 w-56 shrink-0"
            />
            <Select
                value={statusFilter}
                onValueChange={value => onStatusFilterChange(value as PortalsStatusFilter)}
            >
                <SelectTrigger className="h-9 w-auto shrink-0 min-w-28" aria-label="Filter by status">
                    <SelectValue placeholder="Status" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="all">All statuses</SelectItem>
                    <SelectItem value="Published">Published</SelectItem>
                    <SelectItem value="Draft">Draft</SelectItem>
                </SelectContent>
            </Select>

            <ToggleGroup
                type="single"
                variant="outline"
                size="sm"
                spacing={0}
                value={viewMode}
                onValueChange={nextValue => {
                    if (nextValue === 'cards' || nextValue === 'table') {
                        onViewModeChange(nextValue);
                    }
                }}
                aria-label="Portals view mode"
                className="ml-auto"
            >
                <ToggleGroupItem value="cards" aria-label="Card view" title="Card view">
                    <LayoutGridIcon className="size-4" aria-hidden="true" />
                </ToggleGroupItem>
                <ToggleGroupItem value="table" aria-label="Table view" title="Table view">
                    <ListIcon className="size-4" aria-hidden="true" />
                </ToggleGroupItem>
            </ToggleGroup>
        </div>
    );
}
