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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import { MODULE_CONFIG_SECTION_META } from '../types';

type LogLevel = 'INFO' | 'WARN' | 'ERROR';
type TimeRange = '1h' | '24h' | '7d' | '30d';

interface LogEntry {
    readonly id: string;
    readonly timestamp: string;
    readonly level: LogLevel;
    readonly actor: string;
    readonly action: string;
    readonly portal: string;
    readonly payload: Record<string, unknown>;
}

const SAMPLE_LOGS: readonly LogEntry[] = [
    {
        id: 'log-1',
        timestamp: '2026-07-21T08:12:04Z',
        level: 'INFO',
        actor: 'admin@acme.io',
        action: 'portal.published',
        portal: 'Payments API Portal',
        payload: { portalId: 'p-payments', version: '1.4.2' },
    },
    {
        id: 'log-2',
        timestamp: '2026-07-21T07:55:18Z',
        level: 'INFO',
        actor: 'jane.doe@acme.io',
        action: 'subscription.created',
        portal: 'Payments API Portal',
        payload: { applicationId: 'app-42', apiId: 'payments-api' },
    },
    {
        id: 'log-3',
        timestamp: '2026-07-21T06:41:02Z',
        level: 'WARN',
        actor: 'system',
        action: 'domain.ssl.expiring',
        portal: 'Internal Dev Portal',
        payload: { hostname: 'dev.internal.acme.io', daysRemaining: 12 },
    },
    {
        id: 'log-4',
        timestamp: '2026-07-20T22:18:44Z',
        level: 'ERROR',
        actor: 'webhook-worker',
        action: 'webhook.delivery_failed',
        portal: 'Active Fitness Partner APIs',
        payload: { webhookId: 'wh-2', statusCode: 502 },
    },
    {
        id: 'log-5',
        timestamp: '2026-07-20T18:03:11Z',
        level: 'INFO',
        actor: 'alex.kim@partner.io',
        action: 'user.signed_up',
        portal: 'Active Fitness Partner APIs',
        payload: { userId: 'u-991', provider: 'OIDC' },
    },
    {
        id: 'log-6',
        timestamp: '2026-07-19T14:27:50Z',
        level: 'INFO',
        actor: 'admin@acme.io',
        action: 'template.updated',
        portal: '—',
        payload: { templateId: 'tpl-system-home' },
    },
];

function levelBadgeClass(level: LogLevel): string {
    if (level === 'ERROR') {
        return 'bg-destructive/15 text-destructive';
    }
    if (level === 'WARN') {
        return 'bg-amber-500/15 text-amber-700 dark:text-amber-400';
    }
    return 'bg-muted text-muted-foreground';
}

export function LogsPage() {
    const [level, setLevel] = useState<'all' | LogLevel>('all');
    const [timeRange, setTimeRange] = useState<TimeRange>('7d');
    const [search, setSearch] = useState('');
    const [selectedLog, setSelectedLog] = useState<LogEntry | null>(null);

    const filteredLogs = useMemo(() => {
        const query = search.trim().toLowerCase();
        return SAMPLE_LOGS.filter(entry => {
            if (level !== 'all' && entry.level !== level) {
                return false;
            }
            if (!query) {
                return true;
            }
            return (
                entry.actor.toLowerCase().includes(query) ||
                entry.action.toLowerCase().includes(query) ||
                entry.portal.toLowerCase().includes(query)
            );
        });
    }, [level, search]);

    const meta = MODULE_CONFIG_SECTION_META.logs;

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                <p className="text-sm text-muted-foreground">{meta.description}</p>
            </div>

            <Card>
                <CardContent className="flex flex-wrap items-end gap-4 pt-6">
                    <Field>
                        <FieldLabel htmlFor="logs-level">Level</FieldLabel>
                        <select
                            id="logs-level"
                            value={level}
                            onChange={event => setLevel(event.target.value as 'all' | LogLevel)}
                            className="h-9 w-full min-w-[10rem] rounded-md border border-input bg-background px-3 text-sm"
                        >
                            <option value="all">All</option>
                            <option value="INFO">INFO</option>
                            <option value="WARN">WARN</option>
                            <option value="ERROR">ERROR</option>
                        </select>
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="logs-range">Time range</FieldLabel>
                        <select
                            id="logs-range"
                            value={timeRange}
                            onChange={event => setTimeRange(event.target.value as TimeRange)}
                            className="h-9 w-full min-w-[10rem] rounded-md border border-input bg-background px-3 text-sm"
                        >
                            <option value="1h">Last hour</option>
                            <option value="24h">Last 24 hours</option>
                            <option value="7d">Last 7 days</option>
                            <option value="30d">Last 30 days</option>
                        </select>
                    </Field>
                    <div className="min-w-[16rem] flex-1">
                        <Field>
                            <FieldLabel htmlFor="logs-search">Search</FieldLabel>
                            <Input
                                id="logs-search"
                                value={search}
                                onChange={event => setSearch(event.target.value)}
                                placeholder="Actor, action, or portal"
                            />
                        </Field>
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[48rem] border-collapse text-left text-sm">
                            <caption className="sr-only">
                                Portal audit and access logs ({timeRange})
                            </caption>
                            <thead className="border-b border-border/70 bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
                                <tr>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Timestamp
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Level
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Actor
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Action
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Portal
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Details
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredLogs.length === 0 ? (
                                    <tr>
                                        <td colSpan={6} className="px-5 py-10 text-center text-muted-foreground">
                                            No logs match the current filters.
                                        </td>
                                    </tr>
                                ) : (
                                    filteredLogs.map(entry => (
                                        <tr key={entry.id} className="border-b border-border/60 last:border-b-0">
                                            <td className="px-5 py-4 align-middle whitespace-nowrap text-muted-foreground">
                                                {entry.timestamp}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <span
                                                    className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${levelBadgeClass(entry.level)}`}
                                                >
                                                    {entry.level}
                                                </span>
                                            </td>
                                            <td className="px-5 py-4 align-middle">{entry.actor}</td>
                                            <td className="px-5 py-4 align-middle font-medium">{entry.action}</td>
                                            <td className="px-5 py-4 align-middle text-muted-foreground">
                                                {entry.portal}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => setSelectedLog(entry)}
                                                >
                                                    View
                                                </Button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            <Dialog
                open={selectedLog !== null}
                onOpenChange={open => {
                    if (!open) {
                        setSelectedLog(null);
                    }
                }}
            >
                <DialogContent style={{ width: 'min(92vw, 36rem)' }}>
                    <DialogHeader>
                        <DialogTitle>{selectedLog?.action ?? 'Log details'}</DialogTitle>
                        <DialogDescription>
                            {selectedLog
                                ? `${selectedLog.timestamp} · ${selectedLog.actor}`
                                : 'Log payload'}
                        </DialogDescription>
                    </DialogHeader>
                    <pre className="max-h-80 overflow-auto rounded-md border border-border/70 bg-muted/30 p-4 text-xs whitespace-pre-wrap">
                        {selectedLog ? JSON.stringify(selectedLog.payload, null, 2) : ''}
                    </pre>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setSelectedLog(null)}>
                            Close
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
