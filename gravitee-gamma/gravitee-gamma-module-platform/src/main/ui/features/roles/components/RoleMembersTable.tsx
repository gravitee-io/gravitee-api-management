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
import { Button, DataTableEmptyState, DataTablePagination, Skeleton } from '@gravitee/graphene-core';
import { Trash2Icon, UsersIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

import { ClientSideTableSearchField } from '../../../shared/components/ClientSideTableSearchField';
import { useClientSideTableState } from '../../../shared/hooks/useClientSideTableState';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../../shared/utils/paginationConstants';
import type { RoleMembershipListItem } from '../types/role';

const MEMBER_SEARCH_IGNORE_KEYS = ['id', 'role'] as const;

// A role's membership list only carries one meaningful, per-member field (displayName) — not enough for a
// data table's columns, so this renders as a simple list instead (see DataTable usage elsewhere for the
// multi-column case).
export function RoleMembersTable({
    members,
    isLoading,
    canManage,
    onDeleteMember,
}: Readonly<{
    members: RoleMembershipListItem[];
    isLoading: boolean;
    canManage: boolean;
    onDeleteMember: (member: RoleMembershipListItem) => void;
}>) {
    const searchInputId = useId();
    const {
        search,
        page,
        pageSize,
        totalCount,
        paginatedItems: paginatedMembers,
        handleSearchChange,
        handlePageSizeChange,
        setPage,
    } = useClientSideTableState(members, [...MEMBER_SEARCH_IGNORE_KEYS]);

    if (isLoading) {
        return (
            <div className="space-y-3">
                <Skeleton className="h-9 w-64 rounded-md" />
                <div className="space-y-2 rounded-md border p-3">
                    <Skeleton className="h-10 w-full rounded" />
                    <Skeleton className="h-10 w-full rounded" />
                </div>
            </div>
        );
    }

    if (members.length === 0) {
        return (
            <div className="rounded-md border">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<UsersIcon className="size-8" aria-hidden />}
                    title="No member"
                    description="No one has been added to this role yet."
                />
            </div>
        );
    }

    return (
        <div className="space-y-3">
            <DataTablePagination
                page={page}
                pageSize={pageSize}
                totalCount={totalCount}
                pageSizeOptions={[...TABLE_PAGE_SIZE_OPTIONS]}
                onPageChange={setPage}
                onPageSizeChange={handlePageSizeChange}
            >
                <ClientSideTableSearchField id={searchInputId} label="Search members" value={search} onChange={handleSearchChange} />
            </DataTablePagination>

            {paginatedMembers.length === 0 ? (
                <div className="rounded-md border">
                    <DataTableEmptyState
                        variant="no-results"
                        title="No member matches your search."
                        description="Try adjusting your search terms."
                        action={
                            <Button size="sm" variant="outline" onClick={() => handleSearchChange('')}>
                                Clear search
                            </Button>
                        }
                    />
                </div>
            ) : (
                <ul aria-label="Members list" className="divide-y rounded-md border">
                    {paginatedMembers.map(member => (
                        <li key={member.id} className="flex items-center justify-between gap-4 px-4 py-2.5">
                            <span className="text-sm font-medium">{member.displayName}</span>
                            {canManage ? (
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    aria-label="Button to delete a member"
                                    title="Delete member"
                                    onClick={() => onDeleteMember(member)}
                                >
                                    <Trash2Icon className="size-4" aria-hidden />
                                </Button>
                            ) : null}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
