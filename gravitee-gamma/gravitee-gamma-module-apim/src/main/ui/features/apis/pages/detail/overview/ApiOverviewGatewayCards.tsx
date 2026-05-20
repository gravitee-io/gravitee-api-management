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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { GlobeIcon, ServerIcon } from '@gravitee/graphene-core/icons';

import { CopyButton } from '../entrypoints/CopyButton';

interface GatewayUrlCardProps {
    icon: typeof GlobeIcon;
    label: string;
    url: string | undefined;
    isLoading: boolean;
}

function GatewayUrlCard({ icon: Icon, label, url, isLoading }: Readonly<GatewayUrlCardProps>) {
    return (
        <Card className="flex-1">
            <CardContent className="p-5">
                <div className="flex items-center gap-2 mb-3">
                    <Icon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                    <p className="text-sm font-medium">{label}</p>
                </div>
                {isLoading ? (
                    <Skeleton className="h-9 w-full rounded-lg" />
                ) : url ? (
                    <div
                        className="flex items-start justify-between rounded-lg px-3 py-2 gap-3"
                        style={{ backgroundColor: 'color-mix(in oklab, var(--color-muted) 40%, transparent)' }}
                    >
                        <p className="text-sm font-mono min-w-0" style={{ wordBreak: 'break-all' }}>
                            {url}
                        </p>
                        <CopyButton value={url} />
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground italic">Not configured</p>
                )}
            </CardContent>
        </Card>
    );
}

interface ApiOverviewGatewayCardsProps {
    gatewayUrl: string | undefined;
    upstreamUrl: string | undefined;
    isLoadingEntrypoints: boolean;
}

export function ApiOverviewGatewayCards({ gatewayUrl, upstreamUrl, isLoadingEntrypoints }: Readonly<ApiOverviewGatewayCardsProps>) {
    return (
        <div className="flex gap-4">
            <GatewayUrlCard icon={GlobeIcon} label="Gateway Endpoint" url={gatewayUrl} isLoading={isLoadingEntrypoints} />
            <GatewayUrlCard icon={ServerIcon} label="Upstream Service" url={upstreamUrl} isLoading={false} />
        </div>
    );
}
