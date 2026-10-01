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
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    toast,
} from '@gravitee/graphene-core';
import { Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';

import { CloudAccountSettingsHeader } from '../components/CloudAccountSettingsHeader';
import { CloudTokensEmptyState } from '../components/CloudTokensEmptyState';
import { CLOUD_ACCOUNT_TOKENS_MOCK, MAX_ACCOUNT_TOKENS, type AccountToken } from '../cloud.config';

export function CloudAccountTokensPage() {
    const [tokens, setTokens] = useState<readonly AccountToken[]>(CLOUD_ACCOUNT_TOKENS_MOCK);

    const isTokenLimitReached = tokens.length >= MAX_ACCOUNT_TOKENS;

    const formattedTokens = useMemo(
        () =>
            tokens.map(token => ({
                ...token,
                createdAtLabel: new Date(token.createdAt).toLocaleString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                }),
            })),
        [tokens],
    );

    const handleGenerate = useCallback(() => {
        toast.info('Generate Account Token will be available when the Cloud API is connected.');
    }, []);

    const handleDelete = useCallback((tokenName: string) => {
        setTokens(current => current.filter(token => token.tokenName !== tokenName));
        toast.success('Account token has been deleted');
    }, []);

    return (
        <div className="max-w-screen-xl space-y-6">
            <CloudAccountSettingsHeader title="Account Tokens" />

            <Card>
                <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <CardTitle className="text-base">Your Account Tokens</CardTitle>
                    <Button
                        onClick={handleGenerate}
                        disabled={isTokenLimitReached}
                        data-testid="generate-token-button"
                        title={isTokenLimitReached ? `A maximum of ${MAX_ACCOUNT_TOKENS} account tokens can be active at the same time.` : undefined}
                    >
                        Generate Account Token
                    </Button>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Account tokens enable interaction with the Gravitee Cloud REST API, facilitating the automation of various
                        Gravitee Cloud tasks, including creating environments and organizations.
                    </p>
                    <p className="text-sm text-muted-foreground">
                        A maximum of {MAX_ACCOUNT_TOKENS} account tokens can be active at the same time.
                    </p>

                    {formattedTokens.length === 0 ? (
                        <CloudTokensEmptyState testId="account-tokens-empty" />
                    ) : (
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Name</TableHead>
                                    <TableHead>Date created</TableHead>
                                    <TableHead>Created by</TableHead>
                                    <TableHead className="w-16 text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {formattedTokens.map(token => (
                                    <TableRow key={token.id} data-testid={`account-token-row-${token.id}`}>
                                        <TableCell className="font-medium">{token.tokenName}</TableCell>
                                        <TableCell>{token.createdAtLabel}</TableCell>
                                        <TableCell>{token.creatorUserName}</TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-sm"
                                                onClick={() => handleDelete(token.tokenName)}
                                                aria-label={`Delete ${token.tokenName}`}
                                            >
                                                <Trash2Icon aria-hidden />
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
