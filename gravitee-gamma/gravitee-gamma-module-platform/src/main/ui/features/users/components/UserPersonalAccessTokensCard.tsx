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
    DataTable,
    DataTablePagination,
    DateCell,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyMedia,
    EmptyTitle,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { KeyIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useMemo, useState } from 'react';

import { ClientSideTableSearchField } from './ClientSideTableSearchField';
import { GenerateUserTokenDialog } from './GenerateUserTokenDialog';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useClientSideTableState } from '../../../shared/hooks/useClientSideTableState';
import { notify } from '../../../shared/notify';
import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import { useOrganizationUserTokens } from '../hooks/useOrganizationUser';
import { useRevokeOrganizationUserToken } from '../hooks/useUserMutations';
import type { OrganizationUserToken } from '../types/user';
import { formatTokenTimestamp } from '../utils/userTokenDisplay';

const TOKEN_SEARCH_IGNORE_KEYS = ['id', 'token'] as const;

interface UserPersonalAccessTokensCardProps {
    readonly userId: string;
    readonly environmentId: string;
    readonly canGenerate: boolean;
    readonly canRevoke: boolean;
}

function buildColumns(
    canRevoke: boolean,
    onRevokeToken?: (token: OrganizationUserToken) => void,
): DataTableProps<OrganizationUserToken>['columns'] {
    const columns: DataTableProps<OrganizationUserToken>['columns'] = [
        {
            id: 'name',
            accessorFn: (token: OrganizationUserToken) => token.name,
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell<OrganizationUserToken>) => <span className="font-medium text-foreground">{row.original.name}</span>,
        },
        {
            id: 'createdAt',
            accessorFn: (token: OrganizationUserToken) => token.created_at ?? 0,
            header: 'Created at',
            enableSorting: false,
            cell: ({ row }: ColCell<OrganizationUserToken>) => {
                const createdAt = row.original.created_at;
                return createdAt ? <DateCell value={new Date(createdAt)} /> : <span className="text-muted-foreground">—</span>;
            },
        },
        {
            id: 'lastUseAt',
            accessorFn: (token: OrganizationUserToken) => token.last_use_at ?? 0,
            header: 'Last use',
            enableSorting: false,
            cell: ({ row }: ColCell<OrganizationUserToken>) => (
                <span className="text-sm text-muted-foreground">{formatTokenTimestamp(row.original.last_use_at)}</span>
            ),
        },
    ];

    if (canRevoke && onRevokeToken) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            cell: ({ row }: ColCell<OrganizationUserToken>) => (
                <div className="flex justify-end">
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 text-muted-foreground hover:text-destructive"
                        aria-label={`Revoke token ${row.original.name}`}
                        onClick={() => onRevokeToken(row.original)}
                    >
                        <Trash2Icon className="size-4" aria-hidden />
                    </Button>
                </div>
            ),
        });
    }

    return columns;
}

export function UserPersonalAccessTokensCard({ userId, environmentId, canGenerate, canRevoke }: UserPersonalAccessTokensCardProps) {
    const searchInputId = useId();
    const { data: tokens = [], isLoading } = useOrganizationUserTokens(userId);
    const revokeToken = useRevokeOrganizationUserToken(userId);
    const [generateOpen, setGenerateOpen] = useState(false);
    const [tokenToRevoke, setTokenToRevoke] = useState<OrganizationUserToken | null>(null);

    useEffect(() => {
        setGenerateOpen(false);
        setTokenToRevoke(null);
    }, [userId]);

    const columns = useMemo(() => buildColumns(canRevoke, token => setTokenToRevoke(token)), [canRevoke]);
    const {
        search,
        page,
        pageSize,
        totalCount,
        paginatedItems: paginatedTokens,
        hasActiveSearch,
        handleSearchChange,
        handlePageSizeChange,
        setPage,
    } = useClientSideTableState(tokens, TOKEN_SEARCH_IGNORE_KEYS, { resetWhen: userId });
    const activeTokenCount = tokens.length;
    const subtitle = activeTokenCount === 0 ? 'No active tokens' : `${activeTokenCount} active token${activeTokenCount === 1 ? '' : 's'}`;

    function handleRevokeConfirm() {
        if (!tokenToRevoke) {
            return;
        }
        revokeToken.mutate(tokenToRevoke.id, {
            onSuccess: () => {
                setTokenToRevoke(null);
                notify.success('Token successfully deleted!');
            },
            onError: error => {
                notify.error(error, 'Failed to revoke token.');
            },
        });
    }

    return (
        <>
            <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0 pb-3">
                    <div className="space-y-1">
                        <CardTitle className="text-base">Personal Access Tokens</CardTitle>
                        <CardDescription>{subtitle}</CardDescription>
                    </div>
                    {canGenerate && tokens.length > 0 ? (
                        <Button type="button" size="sm" variant="outline" onClick={() => setGenerateOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden />
                            Generate Token
                        </Button>
                    ) : null}
                </CardHeader>
                <CardContent>
                    {isLoading ? (
                        <Skeleton className="h-32 w-full rounded-lg" />
                    ) : tokens.length === 0 ? (
                        <Empty className="border border-dashed rounded-lg py-10">
                            <EmptyHeader>
                                <EmptyMedia variant="icon">
                                    <KeyIcon className="size-5" aria-hidden />
                                </EmptyMedia>
                                <EmptyTitle>No personal access tokens</EmptyTitle>
                                <EmptyDescription>Generate one for API or CI/CD access.</EmptyDescription>
                            </EmptyHeader>
                            {canGenerate ? (
                                <Button type="button" size="sm" onClick={() => setGenerateOpen(true)}>
                                    <PlusIcon className="size-4" aria-hidden />
                                    Generate Token
                                </Button>
                            ) : null}
                        </Empty>
                    ) : (
                        <section className="space-y-3" aria-label="Personal access tokens table">
                            <DataTablePagination
                                page={page}
                                pageSize={pageSize}
                                totalCount={totalCount}
                                pageSizeOptions={[...TABLE_PAGE_SIZE_OPTIONS]}
                                onPageChange={setPage}
                                onPageSizeChange={handlePageSizeChange}
                            >
                                <ClientSideTableSearchField
                                    id={searchInputId}
                                    label="Search tokens"
                                    value={search}
                                    onChange={handleSearchChange}
                                />
                            </DataTablePagination>
                            <DataTable
                                columns={columns}
                                data={paginatedTokens}
                                serverSide
                                skeletonCount={pageSize}
                                emptyMessage={hasActiveSearch ? 'No tokens match your search.' : 'No personal access tokens'}
                            />
                        </section>
                    )}
                </CardContent>
            </Card>

            {canGenerate ? (
                <GenerateUserTokenDialog open={generateOpen} userId={userId} environmentId={environmentId} onOpenChange={setGenerateOpen} />
            ) : null}

            {tokenToRevoke ? (
                <ConfirmDialog
                    open
                    destructive
                    title="Revoke a token"
                    description={
                        <>
                            Are you sure you want to revoke the token <strong>{tokenToRevoke.name}</strong>?
                        </>
                    }
                    confirmLabel="Revoke"
                    pendingLabel="Revoking…"
                    isPending={revokeToken.isPending}
                    onOpenChange={open => !open && !revokeToken.isPending && setTokenToRevoke(null)}
                    onConfirm={handleRevokeConfirm}
                />
            ) : null}
        </>
    );
}
