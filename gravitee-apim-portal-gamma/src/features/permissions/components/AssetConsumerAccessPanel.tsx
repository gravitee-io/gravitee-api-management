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
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';

import type { AssetConsumerGrantRow } from '../hooks/useAssetConsumerGrants';
import { PORTAL_ACCESS_LEVEL_LABELS } from '../types/permissions.types';

interface AssetConsumerAccessPanelProps {
    readonly rows: readonly AssetConsumerGrantRow[];
    /** Rendered next to the heading, typically a link to the portals module Permissions screen. */
    readonly action?: React.ReactNode;
}

export function AssetConsumerAccessPanel({ rows, action }: AssetConsumerAccessPanelProps) {
    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-muted-foreground">
                    Portal groups that can already see or subscribe to this asset. Managed in the developer
                    portals module.
                </p>
                {action}
            </div>

            <div className="overflow-hidden rounded-lg border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Group</TableHead>
                            <TableHead className="w-48">Tenant</TableHead>
                            <TableHead className="w-28">Members</TableHead>
                            <TableHead className="w-40">Access</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {rows.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={4} className="py-8 text-center text-muted-foreground">
                                    No portal group has access yet.
                                </TableCell>
                            </TableRow>
                        ) : (
                            rows.map(row => (
                                <TableRow key={row.grantId}>
                                    <TableCell className="font-medium">{row.groupName}</TableCell>
                                    <TableCell className="text-muted-foreground">{row.tenantName}</TableCell>
                                    <TableCell className="text-muted-foreground">{row.memberCount}</TableCell>
                                    <TableCell>
                                        <span className="flex flex-wrap items-center gap-2">
                                            <Badge variant={row.access === 'CONSUME' ? 'success' : 'secondary'}>
                                                {PORTAL_ACCESS_LEVEL_LABELS[row.access]}
                                            </Badge>
                                            {row.provisioning === 'AUTO' && (
                                                <Badge variant="outline">Auto-provisioned</Badge>
                                            )}
                                        </span>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>
        </div>
    );
}
