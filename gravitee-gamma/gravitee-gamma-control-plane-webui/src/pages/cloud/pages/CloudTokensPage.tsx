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

import { CloudTokensEmptyState } from '../components/CloudTokensEmptyState';
import { CLOUD_CLOUD_TOKENS_MOCK, type CloudToken } from '../cloud.config';

export function CloudTokensPage() {
    const [tokens, setTokens] = useState<readonly CloudToken[]>(CLOUD_CLOUD_TOKENS_MOCK);

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
        toast.info('Generate Cloud Token will be available when the Cloud API is connected.');
    }, []);

    const handleDelete = useCallback((name: string) => {
        setTokens(current => current.filter(token => token.name !== name));
        toast.success('Cloud token has been deleted');
    }, []);

    return (
        <div className="max-w-screen-xl space-y-6">
            <h1 className="text-2xl font-bold tracking-tight">Cloud Tokens</h1>

            <Card>
                <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <CardTitle className="text-base">Your Cloud Tokens</CardTitle>
                    <Button onClick={handleGenerate} data-testid="generate-cloud-token-button">
                        Generate Cloud Token
                    </Button>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Cloud tokens are secure, signed Json Web Tokens (JWT) that enable connection between your self-hosted services and
                        the Gravitee Cloud API Management Control Plane. They are used for:
                    </p>
                    <ul className="list-disc space-y-1 pl-5 text-sm text-muted-foreground">
                        <li>Importing APIs from other Gateways via Gravitee&apos;s Federated API Management capability.</li>
                        <li>Managing APIs across your API Management Environments in Gravitee Cloud using automation tools.</li>
                    </ul>

                    {formattedTokens.length === 0 ? (
                        <CloudTokensEmptyState />
                    ) : (
                        <Table data-testid="cloud-tokens-list">
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Name</TableHead>
                                    <TableHead>Use</TableHead>
                                    <TableHead>Environment</TableHead>
                                    <TableHead>Date created</TableHead>
                                    <TableHead>Created by</TableHead>
                                    <TableHead className="w-16 text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {formattedTokens.map(token => (
                                    <TableRow key={token.id} data-testid={`cloud-token-row-${token.id}`}>
                                        <TableCell className="font-medium">{token.name}</TableCell>
                                        <TableCell>{token.use}</TableCell>
                                        <TableCell>{token.environmentName}</TableCell>
                                        <TableCell>{token.createdAtLabel}</TableCell>
                                        <TableCell>{token.creatorName}</TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-sm"
                                                onClick={() => handleDelete(token.name)}
                                                aria-label={`Delete ${token.name}`}
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
