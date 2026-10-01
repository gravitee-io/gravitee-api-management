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
} from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';

import type { CloudGateway, CloudGatewayConnectionStatus } from '../cloud.config';

const CONNECTION_STATUS_LABELS: Record<CloudGatewayConnectionStatus, string> = {
    connected: 'Connected',
    disconnected: 'Not Connected',
    unknown: 'Unknown',
};

function ConnectionBadge({ status }: { readonly status: CloudGatewayConnectionStatus }) {
    const label = CONNECTION_STATUS_LABELS[status];
    if (status === 'connected') {
        return (
            <Badge className="gap-1 bg-emerald-100 text-emerald-800 hover:bg-emerald-100">
                {label}
                <InfoIcon className="size-3.5" aria-hidden />
            </Badge>
        );
    }
    if (status === 'disconnected') {
        return (
            <Badge variant="destructive" className="gap-1">
                {label}
                <InfoIcon className="size-3.5" aria-hidden />
            </Badge>
        );
    }
    return <Badge variant="secondary">{label}</Badge>;
}

interface CloudGatewaysCardProps {
    readonly gateways: readonly CloudGateway[];
    readonly canDeployGateway?: boolean;
    readonly hasEnvironments?: boolean;
    readonly onDeployGateway?: () => void;
}

export function CloudGatewaysCard({
    gateways,
    canDeployGateway = true,
    hasEnvironments = true,
    onDeployGateway,
}: CloudGatewaysCardProps) {
    const empty = gateways.length === 0;

    return (
        <Card data-testid="cloud-home-gateways">
            <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0 space-y-1.5">
                    <CardTitle className="text-lg">Gateways</CardTitle>
                    <CardDescription>
                        {empty ? 'Manage how your Gateways will be configured.' : 'Manage how your Gateways will be deployed.'}
                    </CardDescription>
                </div>
                <div className="flex shrink-0 items-center gap-2 self-end">
                    <Button
                        data-testid="deploy-gateways-button"
                        disabled={!canDeployGateway || !hasEnvironments}
                        onClick={onDeployGateway}
                    >
                        Deploy Gateway
                    </Button>
                </div>
            </CardHeader>
            <CardContent>
                {empty ? (
                    <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
                        <p className="text-sm font-medium">No Gateways... yet</p>
                        <p className="text-sm text-muted-foreground">Before you get going, you will have to configure your Gateways.</p>
                    </div>
                ) : (
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-foreground text-background hover:bg-foreground">
                                <TableHead className="text-background">Name</TableHead>
                                <TableHead className="text-background">Configuration</TableHead>
                                <TableHead className="text-background">Provider</TableHead>
                                <TableHead className="text-background">Region</TableHead>
                                <TableHead className="text-background">Version</TableHead>
                                <TableHead className="text-background">Environment</TableHead>
                                <TableHead className="text-background">
                                    <span className="inline-flex items-center gap-1">
                                        Status
                                        <InfoIcon className="size-3.5" aria-label="Gateway connection status" />
                                    </span>
                                </TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {gateways.map(gateway => (
                                <TableRow key={gateway.id} data-testid="gateway-list-row">
                                    <TableCell>
                                        <button
                                            type="button"
                                            className="font-medium text-primary hover:underline"
                                            data-testid="gateway-name-link"
                                        >
                                            {gateway.name}
                                        </button>
                                    </TableCell>
                                    <TableCell>{gateway.configuration}</TableCell>
                                    <TableCell>{gateway.provider ?? 'N/A'}</TableCell>
                                    <TableCell>{gateway.region ?? 'N/A'}</TableCell>
                                    <TableCell>
                                        {gateway.version ? (
                                            <Badge variant="secondary" className="bg-orange-100 text-orange-900 hover:bg-orange-100">
                                                {gateway.version}
                                            </Badge>
                                        ) : (
                                            '—'
                                        )}
                                    </TableCell>
                                    <TableCell>{gateway.environmentName}</TableCell>
                                    <TableCell>
                                        <ConnectionBadge status={gateway.connectionStatus} />
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                )}
            </CardContent>
        </Card>
    );
}
