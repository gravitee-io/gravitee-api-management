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
import { Badge, Button, Card, CardContent } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CircleIcon,
    FilterIcon,
    ListIcon,
    LockIcon,
    MonitorIcon,
    PlusIcon,
    RadioIcon,
    ServerIcon,
    WorkflowIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';

// ─── Static data ─────────────────────────────────────────────────────────────

const PROXY_FEATURES: { readonly Icon: LucideIcon; readonly label: string; readonly detail: string }[] = [
    { Icon: ListIcon, label: 'Plans', detail: 'Rate limits & quotas' },
    { Icon: WorkflowIcon, label: 'Flows', detail: 'Request/response paths' },
    { Icon: FilterIcon, label: 'Policies', detail: 'Transform & validate' },
    { Icon: LockIcon, label: 'Security', detail: 'Auth & encryption' },
];

const PROXY_CAPABILITIES = [
    'Authentication & authorization',
    'Rate limiting & quotas',
    'Request/response transformation',
    'Logging & analytics',
    'Load balancing & failover',
] as const;

type EndpointStatus = 'Healthy' | 'Degraded';

const UPSTREAM_ENDPOINTS: { readonly name: string; readonly url: string; readonly status: EndpointStatus }[] = [
    { name: 'Payment Service', url: 'https://api.payments.internal:8443', status: 'Healthy' },
    { name: 'Legacy SOAP API', url: 'https://legacy.soap.internal:9000', status: 'Healthy' },
    { name: 'External Partner API', url: 'https://partner-api.example.com', status: 'Healthy' },
    { name: 'Microservice (gRPC)', url: 'grpc://internal-service:50051', status: 'Degraded' },
];

function ClientsPanel() {
    return (
        <div className="flex-1 rounded-xl border bg-card p-4 space-y-3">
            <div className="flex flex-col items-center text-center gap-2">
                <div className="rounded-xl bg-primary/10 p-2">
                    <MonitorIcon className="size-5 text-primary" aria-hidden />
                </div>
                <div>
                    <p className="text-sm font-semibold">API Clients</p>
                    <p className="text-xs text-muted-foreground">Mobile apps, web apps, and third-party integrations</p>
                </div>
            </div>

            <div className="rounded-lg bg-muted p-2 space-y-1 font-mono text-xs text-muted-foreground">
                <p className="font-sans text-xs font-medium text-foreground">Request</p>
                <p>GET /api/v1/users</p>
                <div className="border-t border-dashed pt-1 mt-1">
                    <p className="font-sans text-xs font-medium text-foreground">Headers</p>
                    <p>Authorization: Bearer ...</p>
                    <p>Content-Type: application/json</p>
                </div>
            </div>

            <div className="flex flex-wrap gap-1">
                <Badge variant="secondary">Mobile Apps</Badge>
                <Badge variant="secondary">Web Applications</Badge>
                <Badge variant="secondary">Third-Party Services</Badge>
            </div>
        </div>
    );
}

function ProxyPanel() {
    return (
        <div className="flex-1 rounded-xl border-2 border-primary/20 bg-primary/5 p-4 space-y-3">
            <div className="flex flex-col items-center text-center gap-2">
                <div className="rounded-xl bg-primary/10 p-2">
                    <RadioIcon className="size-5 text-primary" aria-hidden />
                </div>
                <div>
                    <p className="text-sm font-semibold">API Proxy</p>
                    <p className="text-xs text-muted-foreground">Gravitee Gateway</p>
                </div>
            </div>

            <div className="space-y-1">
                {PROXY_FEATURES.map(({ Icon, label, detail }) => (
                    <div key={label} className="flex items-center gap-2 rounded-lg border bg-card px-3 py-2">
                        <Icon className="size-3 text-primary shrink-0" aria-hidden />
                        <div className="min-w-0">
                            <p className="text-xs font-medium leading-none">{label}</p>
                            <p className="text-xs text-muted-foreground">{detail}</p>
                        </div>
                    </div>
                ))}
            </div>

            <div className="rounded-lg bg-muted p-2 space-y-1">
                <p className="text-xs font-medium text-muted-foreground">What the proxy does:</p>
                <ul className="space-y-1">
                    {PROXY_CAPABILITIES.map(cap => (
                        <li key={cap} className="flex items-center gap-1 text-xs text-muted-foreground">
                            <CircleIcon className="size-2 shrink-0" aria-hidden />
                            {cap}
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}

function EndpointStatusBadge({ status }: { status: EndpointStatus }) {
    if (status === 'Healthy') {
        return (
            <Badge variant="outline" className="ml-auto h-4 border-success/20 text-success shrink-0">
                {status}
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="ml-auto h-4 shrink-0 text-warning border-warning/30">
            {status}
        </Badge>
    );
}

function EndpointsPanel() {
    return (
        <div className="flex-1 space-y-3">
            <p className="text-xs font-medium text-muted-foreground text-center">Upstream Endpoints</p>
            <div className="space-y-2">
                {UPSTREAM_ENDPOINTS.map(({ name, url, status }) => (
                    <div key={name} className="rounded-lg border bg-card px-3 py-2 space-y-1">
                        <div className="flex items-center gap-2">
                            <span
                                className={`size-2 rounded-full shrink-0${status === 'Healthy' ? ' bg-success' : ' bg-warning'}`}
                                aria-hidden
                            />
                            <span className="text-xs font-medium truncate">{name}</span>
                            <EndpointStatusBadge status={status} />
                        </div>
                        <p className="font-mono text-xs text-muted-foreground truncate">{url}</p>
                        <div className="flex items-center gap-1 text-xs text-muted-foreground">
                            <ServerIcon className="size-3 shrink-0" aria-hidden />
                            <span>Backend Service</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

function ArchitectureTile({ icon, title, description }: { icon: ReactNode; title: string; description: string }) {
    return (
        <div className="flex-1 space-y-1">
            <div className="flex items-center gap-2">
                {icon}
                <p className="text-sm font-semibold">{title}</p>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">{description}</p>
        </div>
    );
}

function ProxyArchitectureDiagram() {
    return (
        <Card>
            <CardContent className="pt-6 space-y-6">
                <div>
                    <h3 className="text-base font-semibold">API Proxy Architecture</h3>
                    <p className="text-xs text-muted-foreground mt-1">
                        The proxy acts as an intermediary, adding security, observability, and traffic management between clients and
                        upstream endpoints
                    </p>
                </div>

                {/* Diagram: always horizontal row; arrow wrappers stretch to row height so icons stay centred */}
                <div className="flex flex-row items-stretch gap-3">
                    <ClientsPanel />
                    <div className="flex items-center justify-center shrink-0">
                        <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                    </div>
                    <ProxyPanel />
                    <div className="flex items-center justify-center shrink-0">
                        <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                    </div>
                    <EndpointsPanel />
                </div>

                {/* Bottom tiles */}
                <div className="flex flex-row gap-4 border-t pt-5">
                    <ArchitectureTile
                        icon={
                            <div className="rounded-md bg-muted p-1">
                                <MonitorIcon className="size-3 text-muted-foreground" aria-hidden />
                            </div>
                        }
                        title="Clients"
                        description="Applications and services that consume your API. They make requests through the proxy without knowing about backend complexity."
                    />
                    <ArchitectureTile
                        icon={
                            <div className="rounded-md bg-primary/10 p-1">
                                <RadioIcon className="size-3 text-primary" aria-hidden />
                            </div>
                        }
                        title="Proxy Layer"
                        description="The proxy sits in the middle, handling security, transformation, observability, and routing. This is where you define Plans, Flows, and Policies."
                    />
                    <ArchitectureTile
                        icon={
                            <div className="rounded-md bg-muted p-1">
                                <ServerIcon className="size-3 text-muted-foreground" aria-hidden />
                            </div>
                        }
                        title="Upstream Endpoints"
                        description="Your actual backend services, databases, or third-party APIs. The proxy can route to multiple endpoints with load balancing and failover."
                    />
                </div>
            </CardContent>
        </Card>
    );
}

export function DashboardEmptyLanding({ onCreateProxy }: { onCreateProxy?: () => void }) {
    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Management</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage, secure, and monitor your REST, GraphQL, gRPC, and WebSocket APIs
                    </p>
                </div>
                <Button onClick={onCreateProxy} className="shrink-0">
                    <PlusIcon className="size-4" aria-hidden />
                    Create API proxy
                </Button>
            </div>

            <ProxyArchitectureDiagram />
        </div>
    );
}
