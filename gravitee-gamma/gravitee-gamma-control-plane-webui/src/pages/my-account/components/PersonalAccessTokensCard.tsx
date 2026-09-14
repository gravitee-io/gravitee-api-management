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
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DataTable,
    DataTableEmptyState,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Skeleton,
    toast,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { AlertCircleIcon, KeyIcon, PlusIcon, RefreshCwIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';

import { GeneratePersonalTokenDialog } from './GeneratePersonalTokenDialog';
import { formatTokenTimestamp } from '../myAccount.mapping';
import type { PersonalAccessToken } from '../myAccount.types';
import { listCurrentUserTokens, revokeCurrentUserToken } from '../services/currentUserTokens';

type ColCell = { row: { original: PersonalAccessToken } };

function buildColumns(onRevoke: (token: PersonalAccessToken) => void): DataTableProps<PersonalAccessToken>['columns'] {
    return [
        {
            id: 'name',
            accessorFn: (token: PersonalAccessToken) => token.name,
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell) => <span className="font-medium text-foreground">{row.original.name}</span>,
        },
        {
            id: 'createdAt',
            accessorFn: (token: PersonalAccessToken) => token.created_at ?? 0,
            header: 'Created',
            enableSorting: false,
            cell: ({ row }: ColCell) => <span className="text-muted-foreground">{formatTokenTimestamp(row.original.created_at)}</span>,
        },
        {
            id: 'lastUseAt',
            accessorFn: (token: PersonalAccessToken) => token.last_use_at ?? 0,
            header: 'Last use',
            enableSorting: false,
            cell: ({ row }: ColCell) => <span className="text-muted-foreground">{formatTokenTimestamp(row.original.last_use_at)}</span>,
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            cell: ({ row }: ColCell) => (
                <div className="flex justify-end">
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 text-muted-foreground hover:text-destructive"
                        aria-label={`Revoke token ${row.original.name}`}
                        onClick={() => onRevoke(row.original)}
                    >
                        <Trash2Icon className="size-4" aria-hidden />
                    </Button>
                </div>
            ),
        },
    ];
}

export function PersonalAccessTokensCard({ environmentId }: Readonly<{ environmentId: string }>) {
    const [tokens, setTokens] = useState<PersonalAccessToken[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);
    const [generateOpen, setGenerateOpen] = useState(false);
    const [tokenToRevoke, setTokenToRevoke] = useState<PersonalAccessToken | null>(null);
    const [revoking, setRevoking] = useState(false);

    const reload = useCallback(() => {
        setLoading(true);
        setError(null);
        listCurrentUserTokens()
            .then(next => {
                setTokens(next);
                setError(null);
            })
            .catch(err => {
                setTokens([]);
                setError(err instanceof Error ? err : new Error('Failed to load tokens.'));
            })
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        reload();
    }, [reload]);

    const columns = useMemo(() => buildColumns(token => setTokenToRevoke(token)), []);

    async function handleRevoke() {
        if (!tokenToRevoke) {
            return;
        }
        setRevoking(true);
        try {
            await revokeCurrentUserToken(tokenToRevoke.id);
            toast.success(`Token "${tokenToRevoke.name}" has been revoked.`);
            setTokenToRevoke(null);
            reload();
        } catch (err) {
            toast.error(err instanceof Error ? err.message : 'Failed to revoke token');
        } finally {
            setRevoking(false);
        }
    }

    const generateButton = (
        <Button type="button" size="sm" onClick={() => setGenerateOpen(true)}>
            <PlusIcon className="size-4" aria-hidden />
            Generate token
        </Button>
    );

    return (
        <>
            <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                    <div className="space-y-1">
                        <CardTitle className="text-base">Personal access tokens</CardTitle>
                        <CardDescription>
                            Tokens you have generated that can be used to access the Gravitee.io management API.
                        </CardDescription>
                    </div>
                    {!loading && !error && tokens.length > 0 ? (
                        <Button type="button" size="sm" variant="outline" onClick={() => setGenerateOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden />
                            Generate token
                        </Button>
                    ) : null}
                </CardHeader>
                <CardContent>
                    {loading ? (
                        <Skeleton className="h-32 w-full rounded-lg" />
                    ) : error ? (
                        <Alert className="border-destructive/50 bg-destructive/5">
                            <AlertCircleIcon className="size-4 text-destructive" aria-hidden />
                            <AlertDescription className="flex items-center gap-3 text-destructive">
                                <span className="flex-1" role="alert">
                                    {error.message || 'Failed to load tokens.'}
                                </span>
                                <Button type="button" variant="outline" size="sm" onClick={reload}>
                                    <RefreshCwIcon aria-hidden />
                                    Retry
                                </Button>
                            </AlertDescription>
                        </Alert>
                    ) : tokens.length === 0 ? (
                        <div className="rounded-lg border">
                            <DataTableEmptyState
                                variant="first-use"
                                icon={<KeyIcon />}
                                title="No personal access tokens"
                                description="Generate one for API or CI/CD access."
                                primaryAction={generateButton}
                            />
                        </div>
                    ) : (
                        <DataTable columns={columns} data={tokens} emptyMessage="No personal access tokens" />
                    )}
                </CardContent>
            </Card>
            <GeneratePersonalTokenDialog
                open={generateOpen}
                environmentId={environmentId}
                onOpenChange={setGenerateOpen}
                onGenerated={reload}
            />
            <Dialog open={Boolean(tokenToRevoke)} onOpenChange={open => !open && !revoking && setTokenToRevoke(null)}>
                <DialogContent showCloseButton={false}>
                    <DialogHeader>
                        <DialogTitle>Are you sure you want to revoke the token &quot;{tokenToRevoke?.name}&quot;?</DialogTitle>
                        <DialogDescription>
                            Any applications or scripts using this token will no longer be able to access the Gravitee.io management API.
                            You cannot undo this action.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button type="button" variant="outline" disabled={revoking}>
                                Cancel
                            </Button>
                        </DialogClose>
                        <Button type="button" variant="destructive" disabled={revoking} onClick={() => void handleRevoke()}>
                            {revoking ? 'Revoking…' : 'Revoke'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
}
