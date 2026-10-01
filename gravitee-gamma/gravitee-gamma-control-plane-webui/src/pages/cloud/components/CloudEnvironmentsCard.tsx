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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { InfoIcon, LayoutGridIcon, ListIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import type { CloudProduct } from '../cloud.config';
import { NewEnvironmentDialog, type NewEnvironmentPayload } from './NewEnvironmentDialog';
import { TRIAL_CHIP_ACTIVE_STYLE } from './CloudTrialCountdown';

export type ProductFilter = 'all-envs' | 'apim-envs' | 'am-envs';

type DisplayMode = 'list' | 'card';

export interface CloudEnvironmentItem {
    readonly id: string;
    readonly name: string;
    readonly hrid: string;
    readonly product: CloudProduct;
}

interface CloudEnvironmentsCardProps {
    readonly environments: readonly CloudEnvironmentItem[];
    readonly isCustomer: boolean;
    readonly canCreateEnvironment?: boolean;
    readonly envQuota?: { current: number; max: number };
    readonly onEnvironmentCreated?: (payload: NewEnvironmentPayload) => void;
}

const CHIP_BASE = 'h-8 rounded-md border px-3 text-xs font-medium transition-colors';
const CHIP_INACTIVE = `${CHIP_BASE} border-border text-muted-foreground hover:bg-muted hover:text-foreground`;

export function CloudEnvironmentsCard({
    environments,
    isCustomer,
    canCreateEnvironment = false,
    envQuota,
    onEnvironmentCreated,
}: CloudEnvironmentsCardProps) {
    const [displayMode, setDisplayMode] = useState<DisplayMode>('list');
    const [dialogOpen, setDialogOpen] = useState(false);
    const environmentQuotaExceeded = envQuota !== undefined && envQuota.current >= envQuota.max;

    return (
        <Card data-testid="cloud-home-environments">
            <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0 space-y-1.5">
                    <CardTitle className="flex flex-wrap items-center gap-2 text-lg">
                        Environments
                        {envQuota && (
                            <>
                                <Badge variant="secondary" data-testid="card-environment-quota">
                                    {envQuota.current}/{envQuota.max}
                                </Badge>
                                <InfoIcon className="size-4 text-muted-foreground" aria-label="Contact Gravitee to request more" />
                            </>
                        )}
                    </CardTitle>
                    <CardDescription>All the available environments for your products.</CardDescription>
                </div>
                {(isCustomer || canCreateEnvironment) && (
                    <div className="flex shrink-0 items-center gap-2 self-end">
                        {isCustomer && (
                            <ToggleGroup
                                type="single"
                                value={displayMode}
                                onValueChange={value => value && setDisplayMode(value as DisplayMode)}
                                spacing={0}
                                aria-label="Environment display mode"
                                data-testid="env-display-mode-toggle"
                            >
                                <ToggleGroupItem value="list" className={displayMode === 'list' ? CHIP_BASE : CHIP_INACTIVE} style={displayMode === 'list' ? TRIAL_CHIP_ACTIVE_STYLE : undefined} data-testid="env-display-mode-toggle-list">
                                    <ListIcon aria-hidden />
                                </ToggleGroupItem>
                                <ToggleGroupItem value="card" className={displayMode === 'card' ? CHIP_BASE : CHIP_INACTIVE} style={displayMode === 'card' ? TRIAL_CHIP_ACTIVE_STYLE : undefined} data-testid="env-display-mode-toggle-card">
                                    <LayoutGridIcon aria-hidden />
                                </ToggleGroupItem>
                            </ToggleGroup>
                        )}
                        {canCreateEnvironment && (
                            <Button
                                data-testid="new-environment-button"
                                disabled={environmentQuotaExceeded}
                                onClick={() => setDialogOpen(true)}
                            >
                                New Environment
                            </Button>
                        )}
                    </div>
                )}
            </CardHeader>
            <CardContent>
                {environments.length === 0 ? (
                    <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
                        <p className="text-sm font-medium">No Environments</p>
                        <p className="text-sm text-muted-foreground">There are no environments for this account.</p>
                    </div>
                ) : displayMode === 'list' ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead>Product</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {environments.map(env => (
                                <TableRow key={env.id} data-testid="environment-list-row">
                                    <TableCell>
                                        <Link
                                            to={`/environments/${env.hrid}/cloud/dashboard`}
                                            className="font-medium text-primary hover:underline"
                                            data-testid="environment-list-title"
                                        >
                                            {env.name}
                                        </Link>
                                    </TableCell>
                                    <TableCell>
                                        <Badge data-testid="environment-list-product-label">{env.product}</Badge>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                        {environments.map(env => (
                            <Card key={env.id} className="border-border/60">
                                <CardHeader className="pb-2">
                                    <CardTitle className="text-base">{env.name}</CardTitle>
                                    <Badge variant="secondary">{env.product}</Badge>
                                </CardHeader>
                            </Card>
                        ))}
                    </div>
                )}
            </CardContent>
            {canCreateEnvironment && (
                <NewEnvironmentDialog
                    open={dialogOpen}
                    onOpenChange={setDialogOpen}
                    onCreate={payload => onEnvironmentCreated?.(payload)}
                />
            )}
        </Card>
    );
}
