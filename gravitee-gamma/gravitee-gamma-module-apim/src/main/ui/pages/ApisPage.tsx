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
import { PermissionGate, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, AlertTitle, Button, Card, CardContent, cn } from '@gravitee/graphene-core';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { ArrowRight, MoreHorizontal, Plus, Search, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

import { resolveModulePath } from '../config/routes';

function RowActionsMenu(props: { apiName: string; isPublished: boolean }) {
    const { apiName, isPublished } = props;

    const itemClassName = cn(
        'relative flex cursor-default select-none items-center rounded-sm px-2 py-1.5 text-sm outline-none',
        'focus:bg-muted focus:text-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
    );

    return (
        <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
                <Button type="button" variant="ghost" size="icon" aria-label={`Actions for ${apiName}`}>
                    <MoreHorizontal aria-hidden />
                </Button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
                <DropdownMenu.Content
                    sideOffset={6}
                    align="end"
                    className={cn(
                        'z-50 min-w-48 overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-md',
                        'data-[state=open]:animate-in data-[state=closed]:animate-out',
                        'data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
                        'data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95',
                    )}
                >
                    <DropdownMenu.Item className={itemClassName} onSelect={() => {}}>
                        View Details
                    </DropdownMenu.Item>
                    <DropdownMenu.Item className={itemClassName} onSelect={() => {}}>
                        Edit Configuration
                    </DropdownMenu.Item>
                    <DropdownMenu.Item className={itemClassName} onSelect={() => {}}>
                        View Analytics
                    </DropdownMenu.Item>
                    <DropdownMenu.Item className={itemClassName} onSelect={() => {}}>
                        API Documentation
                    </DropdownMenu.Item>
                    <DropdownMenu.Separator className="my-1 h-px bg-muted" />
                    <DropdownMenu.Item
                        className={cn(itemClassName, 'text-destructive focus:text-destructive')}
                        disabled={!isPublished}
                        onSelect={() => {}}
                    >
                        Unpublish API
                    </DropdownMenu.Item>
                </DropdownMenu.Content>
            </DropdownMenu.Portal>
        </DropdownMenu.Root>
    );
}

export function ApisPage() {
    const canCreateApi = useHasPermission({ anyOf: ['environment-api-c'] });
    const [showWhatsNext, setShowWhatsNext] = useState(true);
    const [query, setQuery] = useState('');
    const location = useLocation();

    const { modulePrefix } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);
    const createProxyTo = useMemo(
        () => (modulePrefix ? `/${modulePrefix}/api-proxy-wizard` : '/api-proxy-wizard'),
        [modulePrefix],
    );

    const apis = useMemo(
        () =>
            [
                { name: 'Flight Status REST API', protocol: 'REST', version: 'v2.4', status: 'Published', calls24h: '148K', uptime: '99.9%' },
                { name: 'Turnaround Ops API', protocol: 'REST', version: 'v1.9', status: 'Published', calls24h: '72K', uptime: '99.7%' },
                { name: 'Crew Coordination API', protocol: 'REST', version: 'v1.3', status: 'Published', calls24h: '39K', uptime: '99.6%' },
                { name: 'Weather Impact API', protocol: 'REST', version: 'v2.0', status: 'Published', calls24h: '26K', uptime: '99.8%' },
                { name: 'Cargo Management API', protocol: 'REST', version: 'v3.1', status: 'Published', calls24h: '67K', uptime: '99.9%' },
                { name: 'Baggage Tracking API', protocol: 'REST', version: 'v2.2', status: 'Published', calls24h: '95K', uptime: '99.7%' },
                { name: 'Passenger Rebooking API', protocol: 'REST', version: 'v1.5', status: 'Published', calls24h: '45K', uptime: '99.8%' },
                { name: 'Airport Ops API', protocol: 'REST', version: 'v1.2', status: 'Draft', calls24h: '—', uptime: '—' },
                { name: 'Gate Announcements API', protocol: 'REST', version: 'v1.0', status: 'Draft', calls24h: '—', uptime: '—' },
                { name: 'Catering Orders API', protocol: 'REST', version: 'v1.1', status: 'Published', calls24h: '59K', uptime: '99.8%' },
                { name: 'Maintenance Requests API', protocol: 'REST', version: 'v2.0', status: 'Published', calls24h: '0.9K', uptime: '99.4%' },
                { name: 'Airport Maps API', protocol: 'REST', version: 'v1.0', status: 'Published', calls24h: '0.2K', uptime: '99.3%' },
            ].filter(a => {
                const q = query.trim().toLowerCase();
                if (!q) return true;
                return (
                    a.name.toLowerCase().includes(q) ||
                    a.protocol.toLowerCase().includes(q) ||
                    a.version.toLowerCase().includes(q) ||
                    a.status.toLowerCase().includes(q)
                );
            }),
        [query],
    );

    const totalApis = 12;
    const publishedApis = 10;
    const draftApis = 1;
    const totalCalls24h = '551K';

    return (
        <div className={cn('flex flex-col gap-6 p-6')}>
            <header className="flex items-start justify-between gap-4">
                <div className="space-y-2">
                    <h1 className="font-semibold text-xl">API Proxies</h1>
                    <p className="text-muted-foreground">Manage and monitor your API proxies</p>
                </div>

                {canCreateApi ? (
                    <Button asChild type="button">
                        <Link to={createProxyTo}>
                            <Plus aria-hidden />
                            Create New Proxy
                        </Link>
                    </Button>
                ) : null}
            </header>

            {showWhatsNext ? (
                <Alert className="rounded-xl border p-5 space-y-4 bg-[var(--graphene-success-50)] border-[var(--graphene-success-200)]">
                    <button
                        type="button"
                        className="absolute right-3 top-3 rounded-sm p-1 text-muted-foreground hover:text-foreground"
                        aria-label="Dismiss what's next"
                        onClick={() => setShowWhatsNext(false)}
                    >
                        <X className="size-4" aria-hidden />
                    </button>

                    <div className="flex-1">
                        <AlertTitle className="font-medium">APIs are live — what's next?</AlertTitle>
                        <AlertDescription className="text-muted-foreground text-xs">
                            Your API proxies are running. Here are ways to extend their value.
                        </AlertDescription>

                        <div className="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-3">
                            {[
                                { title: 'Publish to API Catalog', description: 'Make your APIs discoverable across the organization.' },
                                { title: 'Set up Developer Portal', description: 'Let external developers subscribe and consume your APIs.' },
                                { title: 'Expose as MCP Tools', description: 'Turn these APIs into tools that AI agents can discover.' },
                            ].map(item => (
                                <Card key={item.title} className="rounded-lg border-muted bg-background/50">
                                    <CardContent className="p-4">
                                        <div className="flex items-start justify-between gap-4">
                                            <div className="space-y-1">
                                                <div className="text-sm font-medium">{item.title}</div>
                                                <div className="text-xs text-muted-foreground">{item.description}</div>
                                            </div>
                                            <Button type="button" variant="ghost" size="icon" className="shrink-0">
                                                <ArrowRight aria-hidden />
                                            </Button>
                                        </div>
                                    </CardContent>
                                </Card>
                            ))}
                        </div>
                    </div>
                </Alert>
            ) : null}

            <section className="grid grid-cols-1 gap-3 md:grid-cols-4">
                {[
                    { label: 'Total APIs', value: totalApis },
                    { label: 'Published', value: publishedApis },
                    { label: 'Draft', value: draftApis },
                    { label: 'Total Calls (24h)', value: totalCalls24h },
                ].map(kpi => (
                    <Card key={kpi.label} className="rounded-lg border-muted">
                        <CardContent className="p-4">
                            <div className="text-xs text-muted-foreground">{kpi.label}</div>
                            <div className="mt-2 text-2xl font-semibold">{kpi.value}</div>
                        </CardContent>
                    </Card>
                ))}
            </section>

            <section className="space-y-3">
                <div className="relative max-w-sm">
                    <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
                    <input
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        placeholder="Search APIs..."
                        className={cn(
                            'h-9 w-full rounded-md border bg-background px-9 text-sm outline-none',
                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                        )}
                    />
                </div>

                <div className="rounded-lg border border-muted overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-muted/40">
                            <tr className="text-left">
                                <th className="px-4 py-3 font-medium">API Name</th>
                                <th className="px-4 py-3 font-medium">Protocol</th>
                                <th className="px-4 py-3 font-medium">Version</th>
                                <th className="px-4 py-3 font-medium">Status</th>
                                <th className="px-4 py-3 font-medium">Calls (24h)</th>
                                <th className="px-4 py-3 font-medium">Uptime</th>
                                <th className="px-4 py-3 font-medium w-10" aria-label="Actions" />
                            </tr>
                        </thead>
                        <tbody>
                            {apis.map(api => (
                                <tr key={`${api.name}-${api.version}`} className="border-t border-muted hover:bg-muted/20">
                                    <td className="px-4 py-3 font-medium">{api.name}</td>
                                    <td className="px-4 py-3 text-muted-foreground">{api.protocol}</td>
                                    <td className="px-4 py-3 text-muted-foreground">{api.version}</td>
                                    <td className="px-4 py-3">
                                        <span
                                            className={cn(
                                                'inline-flex items-center rounded px-2 py-0.5 text-xs font-medium',
                                                api.status === 'Published'
                                                    ? 'bg-emerald-500/10 text-emerald-700'
                                                    : 'bg-amber-500/10 text-amber-700',
                                            )}
                                        >
                                            {api.status}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-muted-foreground">{api.calls24h}</td>
                                    <td className="px-4 py-3 text-muted-foreground">{api.uptime}</td>
                                    <td className="px-4 py-3 text-right">
                                        <RowActionsMenu apiName={api.name} isPublished={api.status === 'Published'} />
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <PermissionGate anyOf={['environment-api-u']}>
                    <p className="text-xs text-muted-foreground">You can update APIs in this environment.</p>
                </PermissionGate>
            </section>
        </div>
    );
}
