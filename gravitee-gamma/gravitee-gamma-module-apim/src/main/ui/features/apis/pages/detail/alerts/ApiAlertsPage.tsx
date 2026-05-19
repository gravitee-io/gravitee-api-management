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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Button,
    Card,
    CardContent,
    cn,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Skeleton,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import {
    ActivityIcon,
    ArrowRightIcon,
    BellIcon,
    CircleCheckIcon,
    MailIcon,
    MessageSquareIcon,
    MoreHorizontalIcon,
    PlusIcon,
    WebhookIcon,
} from '@gravitee/graphene-core/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';

import { ALERT_RULES } from '../../../constants/alertConstants';
import { deleteAlertTrigger, listAlerts, updateAlertTrigger, alertTriggerToFormData } from '../../../services/alerts';
import type { AlertTrigger } from '../../../types/api';
import { apiAlertKeys } from '../../../utils/queryKeys';

const CAPABILITIES = [
    'Monitor response times, error rates, and throughput',
    'Detect health check status changes on endpoints',
    'Send notifications via email, Slack, or webhooks',
    'Set severity levels: info, warning, or critical',
    'Configure timeframes for notification windows',
    'Apply filters to target specific traffic subsets',
] as const;

// ─── Severity badge ───────────────────────────────────────────────────────────

const SEVERITY_STYLE: Record<string, { background: string; color: string }> = {
    INFO: { background: 'hsl(var(--primary) / 0.1)', color: 'hsl(var(--primary))' },
    WARNING: { background: 'hsl(var(--warning) / 0.1)', color: 'hsl(var(--warning))' },
    CRITICAL: { background: 'hsl(var(--destructive) / 0.1)', color: 'hsl(var(--destructive))' },
};

function SeverityBadge({ severity }: { severity: AlertTrigger['severity'] }) {
    return (
        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium" style={SEVERITY_STYLE[severity] ?? {}}>
            {severity.toLowerCase()}
        </span>
    );
}

function getRuleLabel(source: string, type: string): string {
    const ruleId = `${source}@${type}`;
    return ALERT_RULES.find(r => r.id === ruleId)?.description ?? `${source} / ${type}`;
}

// ─── Empty state ──────────────────────────────────────────────────────────────

function EmptyState() {
    return (
        <Card className="space-y-6 p-6">
            <div className="space-y-1">
                <p className="text-sm font-semibold">Why configure runtime alerts?</p>
                <p className="text-sm text-muted-foreground">
                    Get notified automatically when API traffic anomalies occur — latency spikes, error rate increases, health check
                    failures — without constantly monitoring dashboards.
                </p>
            </div>

            <div className="rounded-xl p-5 bg-primary/10" style={{ border: '2px solid hsl(var(--primary))' }}>
                <p className="mb-4 text-xs font-semibold text-primary">How it works</p>
                <div className="flex items-center justify-center gap-3">
                    <FlowNode icon={ActivityIcon} label="Gateway traffic" />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <FlowNode icon={BellIcon} label="Alert rule" active />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <div className="flex flex-col items-center gap-1.5">
                        <div className="flex gap-1">
                            <div className="rounded-lg bg-muted p-1.5">
                                <MailIcon className="size-3 text-muted-foreground" />
                            </div>
                            <div className="rounded-lg bg-muted p-1.5">
                                <MessageSquareIcon className="size-3 text-muted-foreground" />
                            </div>
                            <div className="rounded-lg bg-muted p-1.5">
                                <WebhookIcon className="size-3 text-muted-foreground" />
                            </div>
                        </div>
                        <p className="text-xs text-muted-foreground">Email · Slack · Webhook</p>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                <div className="space-y-3">
                    <p className="text-xs font-semibold">Available alert rules</p>
                    <ul className="space-y-2.5">
                        {ALERT_RULES.map(rule => (
                            <li key={rule.id} className="flex items-start gap-2">
                                <BellIcon className="mt-0.5 size-3.5 shrink-0 text-primary" aria-hidden="true" />
                                <span className="text-xs text-muted-foreground">
                                    {rule.description} <span className="text-muted-foreground/60">({rule.category})</span>
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="space-y-3">
                    <p className="text-xs font-semibold">Key capabilities</p>
                    <ul className="space-y-2.5">
                        {CAPABILITIES.map(cap => (
                            <li key={cap} className="flex items-start gap-2">
                                <CircleCheckIcon className="mt-0.5 size-3.5 shrink-0 text-success" aria-hidden="true" />
                                <span className="text-xs text-muted-foreground">{cap}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            </div>
        </Card>
    );
}

// ─── Flow node (used in empty state) ─────────────────────────────────────────

function FlowNode({
    icon: Icon,
    label,
    active = false,
}: Readonly<{ icon: React.ComponentType<{ className?: string }>; label: string; active?: boolean }>) {
    return (
        <div className={cn('flex flex-col items-center gap-1.5', active && 'rounded-lg border border-border bg-card px-3 py-2')}>
            <div className={cn('rounded-lg p-2', active ? 'bg-primary/10' : 'bg-muted')}>
                <Icon className={cn('size-4', active ? 'text-primary' : 'text-muted-foreground')} />
            </div>
            <p className={cn('text-center text-xs', active ? 'font-semibold' : 'text-muted-foreground')}>{label}</p>
        </div>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiAlertsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });

    const {
        data: alerts,
        isLoading,
        isError,
    } = useQuery({
        queryKey: apiAlertKeys.list(env?.id ?? '', apiId ?? ''),
        queryFn: () => listAlerts(env?.id ?? '', apiId!),
        enabled: !!apiId,
    });

    const deleteMutation = useMutation({
        mutationFn: (alertId: string) => deleteAlertTrigger(env?.id ?? '', apiId!, alertId),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiAlertKeys.list(env?.id ?? '', apiId ?? '') }),
    });

    const toggleMutation = useMutation({
        mutationFn: async (alert: AlertTrigger) => {
            const formData = alertTriggerToFormData(alert);
            return updateAlertTrigger(env?.id ?? '', apiId!, alert.id!, { ...formData, enabled: !alert.enabled });
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiAlertKeys.list(env?.id ?? '', apiId ?? '') }),
    });

    const handleAdd = () => navigate('new');
    const handleEdit = (alertId: string) => navigate(alertId);

    return (
        <div className="space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Runtime Alerts</h1>
                    <p className="text-sm text-muted-foreground">Set up alerting conditions for the Gateway.</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    {canEdit && (
                        <Button type="button" size="sm" onClick={handleAdd}>
                            <PlusIcon className="size-4" aria-hidden="true" />
                            Add alert
                        </Button>
                    )}
                </div>
            </div>

            {/* Loading */}
            {isLoading && (
                <div className="space-y-3">
                    {[1, 2, 3].map(i => (
                        <Skeleton key={i} className="h-16 w-full rounded-lg" />
                    ))}
                </div>
            )}

            {/* Error */}
            {isError && (
                <Card>
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Failed to load alerts. Please try again.</p>
                    </CardContent>
                </Card>
            )}

            {/* Empty state */}
            {!isLoading && !isError && (!alerts || alerts.length === 0) && <EmptyState />}

            {/* Alerts table */}
            {!isLoading && !isError && alerts && alerts.length > 0 && (
                <div className="rounded-lg border">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead>Rule</TableHead>
                                <TableHead>Severity</TableHead>
                                <TableHead>Enabled</TableHead>
                                {canEdit && <TableHead className="w-12 text-right">Actions</TableHead>}
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {alerts.map(alert => (
                                <TableRow key={alert.id} className="cursor-pointer" onClick={() => handleEdit(alert.id!)}>
                                    <TableCell>
                                        <div>
                                            <p className="text-sm font-medium">{alert.name}</p>
                                            {alert.description && (
                                                <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{alert.description}</p>
                                            )}
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <span className="text-sm text-muted-foreground">{getRuleLabel(alert.source, alert.type)}</span>
                                    </TableCell>
                                    <TableCell>
                                        <SeverityBadge severity={alert.severity} />
                                    </TableCell>
                                    <TableCell>
                                        <Switch
                                            checked={alert.enabled}
                                            disabled={!canEdit || toggleMutation.isPending}
                                            onClick={e => e.stopPropagation()}
                                            onCheckedChange={() => toggleMutation.mutate(alert)}
                                        />
                                    </TableCell>
                                    {canEdit && (
                                        <TableCell onClick={e => e.stopPropagation()}>
                                            <DropdownMenu>
                                                <DropdownMenuTrigger asChild>
                                                    <Button variant="ghost" size="icon" className="size-8">
                                                        <MoreHorizontalIcon className="size-4" />
                                                    </Button>
                                                </DropdownMenuTrigger>
                                                <DropdownMenuContent align="end">
                                                    <DropdownMenuItem onSelect={() => handleEdit(alert.id!)}>Edit</DropdownMenuItem>
                                                    <DropdownMenuSeparator />
                                                    <DropdownMenuItem
                                                        className="text-destructive focus:text-destructive"
                                                        onSelect={() => deleteMutation.mutate(alert.id!)}
                                                    >
                                                        Delete
                                                    </DropdownMenuItem>
                                                </DropdownMenuContent>
                                            </DropdownMenu>
                                        </TableCell>
                                    )}
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>
            )}
        </div>
    );
}
