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
    Badge,
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import {
    ArrowDownIcon,
    ArrowUpDownIcon,
    ArrowUpIcon,
    MoreHorizontalIcon,
    PencilIcon,
    PlugZapIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { PORTAL_IDP_TYPE_LABELS, type TransversalIdentityProvider } from '../types';

type StatusFilter = 'all' | 'active' | 'inactive';
type SortColumn = 'name' | 'status' | 'protocol' | 'portals';
type SortDirection = 'asc' | 'desc';

interface IdentityProvidersTableProps {
    readonly providers: readonly TransversalIdentityProvider[];
    readonly portalNameById: ReadonlyMap<string, string>;
    readonly onEdit: (provider: TransversalIdentityProvider) => void;
    readonly onToggleEnabled: (provider: TransversalIdentityProvider) => void;
    readonly onDelete: (provider: TransversalIdentityProvider) => void;
}

function StatusBadge({ enabled }: { readonly enabled: boolean }) {
    if (enabled) {
        return (
            <Badge variant="outline" className="border-success/30 text-success">
                Active
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="border-destructive/30 text-destructive">
            Inactive
        </Badge>
    );
}

function formatAssignedPortals(
    portalIds: readonly string[],
    portalNameById: ReadonlyMap<string, string>,
): string {
    if (portalIds.length === 0) {
        return 'None';
    }
    return portalIds.map(id => portalNameById.get(id) ?? id).join(', ');
}

function ProviderRowActions({
    provider,
    onEdit,
    onToggleEnabled,
    onDelete,
}: {
    readonly provider: TransversalIdentityProvider;
    readonly onEdit: () => void;
    readonly onToggleEnabled: () => void;
    readonly onDelete: () => void;
}) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    size="icon"
                    className="size-8"
                    aria-label={`Actions for ${provider.name}`}
                >
                    <MoreHorizontalIcon className="size-4" aria-hidden="true" />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="min-w-48">
                <DropdownMenuItem className="gap-2 whitespace-nowrap" onClick={onEdit}>
                    <PencilIcon className="size-4 shrink-0" aria-hidden="true" />
                    Edit
                </DropdownMenuItem>
                <DropdownMenuItem className="gap-2 whitespace-nowrap" onClick={onToggleEnabled}>
                    <PlugZapIcon className="size-4 shrink-0" aria-hidden="true" />
                    {provider.enabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>
                <DropdownMenuItem className="gap-2 whitespace-nowrap text-destructive" onClick={onDelete}>
                    <Trash2Icon className="size-4 shrink-0" aria-hidden="true" />
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

function SortableHeader({
    label,
    column,
    sortColumn,
    sortDirection,
    onSort,
}: {
    readonly label: string;
    readonly column: SortColumn;
    readonly sortColumn: SortColumn;
    readonly sortDirection: SortDirection;
    readonly onSort: (column: SortColumn) => void;
}) {
    const isActive = sortColumn === column;
    const Icon = !isActive ? ArrowUpDownIcon : sortDirection === 'asc' ? ArrowUpIcon : ArrowDownIcon;

    return (
        <th scope="col" className="px-4 py-3 font-medium">
            <button
                type="button"
                className="inline-flex items-center gap-1.5 hover:text-foreground"
                onClick={() => onSort(column)}
                aria-label={`Sort by ${label}`}
            >
                {label}
                <Icon className="size-3.5 text-muted-foreground" aria-hidden />
            </button>
        </th>
    );
}

export function IdentityProvidersTable({
    providers,
    portalNameById,
    onEdit,
    onToggleEnabled,
    onDelete,
}: IdentityProvidersTableProps) {
    const [nameFilter, setNameFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
    const [sortColumn, setSortColumn] = useState<SortColumn>('name');
    const [sortDirection, setSortDirection] = useState<SortDirection>('asc');

    const handleSort = (column: SortColumn) => {
        if (sortColumn === column) {
            setSortDirection(current => (current === 'asc' ? 'desc' : 'asc'));
            return;
        }
        setSortColumn(column);
        setSortDirection('asc');
    };

    const filteredAndSorted = useMemo(() => {
        const query = nameFilter.trim().toLowerCase();
        const filtered = providers.filter(provider => {
            if (statusFilter === 'active' && !provider.enabled) {
                return false;
            }
            if (statusFilter === 'inactive' && provider.enabled) {
                return false;
            }
            if (!query) {
                return true;
            }
            const protocol = PORTAL_IDP_TYPE_LABELS[provider.type].toLowerCase();
            return provider.name.toLowerCase().includes(query) || protocol.includes(query);
        });

        const direction = sortDirection === 'asc' ? 1 : -1;
        return [...filtered].sort((a, b) => {
            let comparison = 0;
            switch (sortColumn) {
                case 'name':
                    comparison = a.name.localeCompare(b.name);
                    break;
                case 'status':
                    comparison = Number(b.enabled) - Number(a.enabled);
                    break;
                case 'protocol':
                    comparison = PORTAL_IDP_TYPE_LABELS[a.type].localeCompare(
                        PORTAL_IDP_TYPE_LABELS[b.type],
                    );
                    break;
                case 'portals':
                    comparison = formatAssignedPortals(a.portalIds, portalNameById).localeCompare(
                        formatAssignedPortals(b.portalIds, portalNameById),
                    );
                    break;
            }
            return comparison * direction;
        });
    }, [nameFilter, portalNameById, providers, sortColumn, sortDirection, statusFilter]);

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3">
                <Input
                    value={nameFilter}
                    onChange={event => setNameFilter(event.target.value)}
                    placeholder="Filter providers..."
                    aria-label="Filter providers by name or protocol"
                    className="h-9 w-56 shrink-0"
                />
                <Select value={statusFilter} onValueChange={value => setStatusFilter(value as StatusFilter)}>
                    <SelectTrigger className="h-9 w-auto shrink-0 min-w-28" aria-label="Filter by status">
                        <SelectValue placeholder="Status" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="all">All statuses</SelectItem>
                        <SelectItem value="active">Active</SelectItem>
                        <SelectItem value="inactive">Inactive</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            <div className="overflow-hidden rounded-lg border">
                <table className="w-full min-w-[48rem] text-sm">
                    <caption className="sr-only">Transversal identity providers</caption>
                    <thead className="bg-muted/40 text-left">
                        <tr>
                            <SortableHeader
                                label="Name"
                                column="name"
                                sortColumn={sortColumn}
                                sortDirection={sortDirection}
                                onSort={handleSort}
                            />
                            <SortableHeader
                                label="Status"
                                column="status"
                                sortColumn={sortColumn}
                                sortDirection={sortDirection}
                                onSort={handleSort}
                            />
                            <SortableHeader
                                label="Protocol Type"
                                column="protocol"
                                sortColumn={sortColumn}
                                sortDirection={sortDirection}
                                onSort={handleSort}
                            />
                            <SortableHeader
                                label="Assigned Portal"
                                column="portals"
                                sortColumn={sortColumn}
                                sortDirection={sortDirection}
                                onSort={handleSort}
                            />
                            <th scope="col" className="px-4 py-3 font-medium">
                                <span className="sr-only">Actions</span>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredAndSorted.length === 0 ? (
                            <tr className="border-t">
                                <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                                    {providers.length === 0
                                        ? 'No transversal identity providers yet. Add one to reuse across portals.'
                                        : 'No providers match your filters.'}
                                </td>
                            </tr>
                        ) : (
                            filteredAndSorted.map(provider => (
                                <tr key={provider.id} className="border-t hover:bg-muted/40">
                                    <td className="px-4 py-3 align-middle font-medium">{provider.name}</td>
                                    <td className="px-4 py-3 align-middle">
                                        <StatusBadge enabled={provider.enabled} />
                                    </td>
                                    <td className="px-4 py-3 align-middle text-muted-foreground">
                                        {PORTAL_IDP_TYPE_LABELS[provider.type]}
                                    </td>
                                    <td className="px-4 py-3 align-middle text-muted-foreground">
                                        {formatAssignedPortals(provider.portalIds, portalNameById)}
                                    </td>
                                    <td className="px-4 py-3 align-middle text-right">
                                        <ProviderRowActions
                                            provider={provider}
                                            onEdit={() => onEdit(provider)}
                                            onToggleEnabled={() => onToggleEnabled(provider)}
                                            onDelete={() => onDelete(provider)}
                                        />
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
