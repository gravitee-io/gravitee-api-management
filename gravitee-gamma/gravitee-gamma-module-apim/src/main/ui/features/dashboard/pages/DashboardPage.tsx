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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { Badge, Button, Card } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CircleIcon,
    FilterIcon,
    GitBranchIcon,
    ListIcon,
    LockIcon,
    MonitorIcon,
    PlusIcon,
    RadioIcon,
    ServerIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

import { APIM_ROUTE_CONFIG } from '../../../config/routes';

export function DashboardPage() {
    const navigate = useNavigate();
    const { rootPath } = useModuleRouting(APIM_ROUTE_CONFIG);
    const handleCreateApiProxy = useCallback(() => {
        navigate(`${rootPath}/new`);
    }, [navigate, rootPath]);

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Management</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage, secure, and monitor your REST, GraphQL, gRPC, and WebSocket APIs
                    </p>
                </div>

                <div className="ml-auto flex items-center gap-2 shrink-0">
                    <Button type="button" size="sm" onClick={handleCreateApiProxy}>
                        <PlusIcon className="mr-2 size-4" aria-hidden="true" />
                        Create API proxy
                    </Button>
                </div>
            </div>

            <Card className="p-6">
                <div className="space-y-6">
                    <div className="space-y-1">
                        <h2 className="text-base font-semibold">API Proxy Architecture</h2>
                        <p className="text-sm text-muted-foreground">
                            The proxy acts as an intermediary, adding security, observability, and traffic management between clients and
                            upstream endpoints.
                        </p>
                    </div>

                    <div className="flex items-start gap-4">
                        <div className="flex-1 rounded-xl border bg-card p-4 space-y-3 self-start">
                            <div className="flex flex-col items-center text-center gap-2">
                                <div className="rounded-xl bg-primary/10 p-2.5">
                                    <MonitorIcon className="size-5 text-primary" aria-hidden="true" />
                                </div>
                                <div>
                                    <p className="text-sm font-semibold">API Clients</p>
                                    <p className="text-[11px] text-muted-foreground">Mobile apps, web apps, and third-party integrations</p>
                                </div>
                            </div>

                            <div className="rounded-lg bg-muted/50 p-2.5 space-y-1.5 font-mono text-[10px] text-muted-foreground">
                                <p className="font-sans text-[11px] font-medium text-foreground">Request</p>
                                <p>GET /api/v1/users</p>
                                <div className="border-t border-dashed pt-1.5 mt-1.5">
                                    <p className="font-sans text-[11px] font-medium text-foreground">Headers</p>
                                    <p>Authorization: Bearer ...</p>
                                    <p>Content-Type: application/json</p>
                                </div>
                            </div>

                            <div className="flex flex-wrap gap-1">
                                <Badge variant="secondary" className="rounded-sm">
                                    Mobile Apps
                                </Badge>
                                <Badge variant="secondary" className="rounded-sm">
                                    Web Applications
                                </Badge>
                                <Badge variant="secondary" className="rounded-sm">
                                    Third-Party Services
                                </Badge>
                            </div>
                        </div>

                        <div className="self-stretch shrink-0 flex items-center justify-center">
                            <ArrowRightIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                        </div>

                        <div className="flex-1 rounded-xl border border-primary bg-primary/10 p-4 space-y-3 self-start">
                            <div className="flex flex-col items-center text-center gap-2">
                                <div className="rounded-xl bg-primary/10 p-2.5">
                                    <RadioIcon className="size-5 text-primary" aria-hidden="true" />
                                </div>
                                <div>
                                    <p className="text-sm font-semibold">API Proxy</p>
                                    <p className="text-[11px] text-muted-foreground">Gravitee Gateway</p>
                                </div>
                            </div>

                            <div className="space-y-2">
                                {[
                                    { icon: ListIcon, label: 'Plans', detail: 'Rate limits & quotas' },
                                    { icon: GitBranchIcon, label: 'Flows', detail: 'Request/response paths' },
                                    { icon: FilterIcon, label: 'Policies', detail: 'Transform & validate' },
                                    { icon: LockIcon, label: 'Security', detail: 'Auth & encryption' },
                                ].map(feature => (
                                    <div key={feature.label} className="flex items-center gap-2.5 rounded-lg border bg-card px-3 py-2">
                                        <feature.icon className="size-3.5 text-primary shrink-0" aria-hidden="true" />
                                        <div className="min-w-0">
                                            <p className="text-xs font-medium leading-none">{feature.label}</p>
                                            <p className="text-[10px] text-muted-foreground">{feature.detail}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="rounded-lg bg-muted/40 p-2.5">
                                <p className="text-[11px] text-muted-foreground font-medium mb-1.5">What the proxy does:</p>
                                <ul className="space-y-0.5 text-[10px] text-muted-foreground">
                                    <li className="flex items-center gap-1.5">
                                        <CircleIcon className="size-2 shrink-0" aria-hidden="true" /> Authentication & authorization
                                    </li>
                                    <li className="flex items-center gap-1.5">
                                        <CircleIcon className="size-2 shrink-0" aria-hidden="true" /> Rate limiting & quotas
                                    </li>
                                    <li className="flex items-center gap-1.5">
                                        <CircleIcon className="size-2 shrink-0" aria-hidden="true" /> Request/response transformation
                                    </li>
                                    <li className="flex items-center gap-1.5">
                                        <CircleIcon className="size-2 shrink-0" aria-hidden="true" /> Logging & analytics
                                    </li>
                                    <li className="flex items-center gap-1.5">
                                        <CircleIcon className="size-2 shrink-0" aria-hidden="true" /> Load balancing & failover
                                    </li>
                                </ul>
                            </div>
                        </div>

                        <div className="self-stretch shrink-0 flex items-center justify-center">
                            <ArrowRightIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                        </div>

                        <div className="flex-1 space-y-3 self-start">
                            <p className="text-xs font-medium text-muted-foreground text-center">Upstream Endpoints</p>
                            <div className="space-y-2">
                                {[
                                    {
                                        name: 'Payment Service',
                                        url: 'https://api.payments.internal:8443',
                                        status: 'Healthy' as const,
                                        color: 'bg-primary',
                                        badgeClassName: 'border-success text-success',
                                    },
                                    {
                                        name: 'Legacy SOAP API',
                                        url: 'https://legacy.soap.internal:9000',
                                        status: 'Healthy' as const,
                                        color: 'bg-primary',
                                        badgeClassName: 'border-success text-success',
                                    },
                                    {
                                        name: 'External Partner API',
                                        url: 'https://partner-api.example.com',
                                        status: 'Healthy' as const,
                                        color: 'bg-primary',
                                        badgeClassName: 'border-success text-success',
                                    },
                                    {
                                        name: 'Microservice (gRPC)',
                                        url: 'grpc://internal-service:50051',
                                        status: 'Degraded' as const,
                                        color: 'bg-primary',
                                        badgeClassName: 'border-warning text-warning',
                                    },
                                ].map(endpoint => (
                                    <div key={endpoint.name} className="rounded-lg border bg-card px-3 py-2.5 space-y-1">
                                        <div className="flex items-center gap-2">
                                            <span className={`size-2 rounded-full ${endpoint.color} shrink-0`} />
                                            <span className="text-xs font-medium truncate">{endpoint.name}</span>
                                            <Badge
                                                variant="outline"
                                                className={`ml-auto bg-background text-xs h-6 px-3 shrink-0 rounded-md ${endpoint.badgeClassName}`}
                                            >
                                                {endpoint.status}
                                            </Badge>
                                        </div>
                                        <p className="font-mono text-[10px] text-muted-foreground truncate">{endpoint.url}</p>
                                        <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
                                            <ServerIcon className="size-2.5" aria-hidden="true" />
                                            <span>Backend Service</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="grid gap-4 grid-cols-3 border-t pt-5 py-6">
                        <div className="space-y-1.5">
                            <div className="flex items-center gap-2">
                                <div className="rounded-md bg-muted p-1">
                                    <MonitorIcon className="size-3.5 text-muted-foreground" aria-hidden="true" />
                                </div>
                                <p className="text-sm font-semibold">Clients</p>
                            </div>
                            <p className="text-[11px] leading-relaxed text-muted-foreground">
                                Applications and services that consume your API. They make requests through the proxy without knowing about
                                backend complexity.
                            </p>
                        </div>
                        <div className="space-y-1.5">
                            <div className="flex items-center gap-2">
                                <div className="rounded-md bg-primary/10 p-1">
                                    <RadioIcon className="size-3.5 text-primary" aria-hidden="true" />
                                </div>
                                <p className="text-sm font-semibold">Proxy Layer</p>
                            </div>
                            <p className="text-[11px] leading-relaxed text-muted-foreground">
                                The proxy sits in the middle, handling security, transformation, observability, and routing. This is where
                                you define Plans, Flows, and Policies.
                            </p>
                        </div>
                        <div className="space-y-1.5">
                            <div className="flex items-center gap-2">
                                <div className="rounded-md bg-muted p-1">
                                    <ServerIcon className="size-3.5 text-muted-foreground" aria-hidden="true" />
                                </div>
                                <p className="text-sm font-semibold">Upstream Endpoints</p>
                            </div>
                            <p className="text-[11px] leading-relaxed text-muted-foreground">
                                Your actual backend services, databases, or third-party APIs. The proxy can route to multiple endpoints with
                                load balancing and failover.
                            </p>
                        </div>
                    </div>
                </div>
            </Card>
        </div>
    );
}
