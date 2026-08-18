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
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { AlertsEducationalEmptyState } from '../features/alerts/components/AlertsEducationalEmptyState';
import { SeverityBadge } from '../features/alerts/components/SeverityBadge';
import { getAlertRuleLabel } from '../features/alerts/constants/alertRules';
import { deletePlatformAlert, listPlatformAlerts, updatePlatformAlert } from '../features/alerts/services/alerts';
import type { AlertTrigger } from '../features/alerts/types/alert';
import {
    formatAlertCounters,
    formatAlertCountersTooltip,
    formatLastAlertAt,
    formatLastAlertMessage,
} from '../features/alerts/utils/alertListFormat';
import {
    ENVIRONMENT_ALERT_CREATE_PERMISSION,
    ENVIRONMENT_ALERT_DELETE_PERMISSION,
    ENVIRONMENT_ALERT_UPDATE_PERMISSION,
} from '../features/alerts/utils/alertPermissions';
import { platformAlertKeys } from '../features/alerts/utils/queryKeys';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { notify } from '../shared/notify';

/** Classic `md-tooltip` on the counters cell: `N during the last 5 minutes / …`. */
function AlertCountersCell({ counters }: { counters: AlertTrigger['counters'] }) {
    const label = formatAlertCounters(counters);
    const tooltip = formatAlertCountersTooltip(counters);

    if (!tooltip) {
        return <span className="text-sm text-muted-foreground">{label}</span>;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="text-sm text-muted-foreground">{label}</span>
            </TooltipTrigger>
            <TooltipContent>{tooltip}</TooltipContent>
        </Tooltip>
    );
}

/**
 * Environment-level Alerts landing page (APIM-14911 / APIM-14912).
 *
 * Loads `/platform/alerts`, shows the educational empty state when none exist,
 * and otherwise lists alerts with enable/disable, edit, and delete.
 */
export function AlertsPage() {
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const environmentId = env?.id ?? '';

    const canCreate = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_CREATE_PERMISSION] });
    const canUpdate = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_UPDATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_DELETE_PERMISSION] });
    const showActions = canUpdate || canDelete;

    const [alertToDelete, setAlertToDelete] = useState<AlertTrigger | null>(null);

    const {
        data: alerts,
        isLoading,
        isError,
    } = useQuery({
        queryKey: platformAlertKeys.list(environmentId),
        queryFn: () => listPlatformAlerts(environmentId),
        enabled: !!environmentId,
    });

    const toggleMutation = useMutation({
        mutationFn: (alert: AlertTrigger) => updatePlatformAlert(environmentId, { ...alert, enabled: !alert.enabled }),
        onSuccess: updated => {
            queryClient.invalidateQueries({ queryKey: platformAlertKeys.list(environmentId) });
            notify.success(updated.enabled ? `Alert "${updated.name}" enabled.` : `Alert "${updated.name}" disabled.`);
        },
        onError: error => {
            notify.error(error, 'Failed to update alert.');
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (alert: AlertTrigger) => deletePlatformAlert(environmentId, alert.id!),
        onSuccess: (_void, alert) => {
            queryClient.invalidateQueries({ queryKey: platformAlertKeys.list(environmentId) });
            notify.success(`Alert "${alert.name}" deleted.`);
            setAlertToDelete(null);
        },
        onError: error => {
            notify.error(error, 'Failed to delete alert.');
        },
    });

    const handleAdd = () => navigate('new');
    const handleEdit = (alertId: string) => navigate(alertId);

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Alerts</h1>
                    <p className="text-sm text-muted-foreground">Get notified when your gateways or platform need attention.</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    {canCreate && (
                        <Button type="button" size="sm" onClick={handleAdd}>
                            <PlusIcon className="size-4" aria-hidden="true" />
                            Add alert
                        </Button>
                    )}
                </div>
            </div>

            {isLoading && (
                <div className="space-y-3">
                    {[1, 2, 3].map(i => (
                        <Skeleton key={i} className="h-16 w-full rounded-lg" />
                    ))}
                </div>
            )}

            {isError && (
                <Card>
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Failed to load alerts. Please try again.</p>
                    </CardContent>
                </Card>
            )}

            {!isLoading && !isError && (!alerts || alerts.length === 0) && <AlertsEducationalEmptyState />}

            {!isLoading && !isError && alerts && alerts.length > 0 && (
                <div className="rounded-lg border">
                    <TooltipProvider delayDuration={200}>
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Name</TableHead>
                                    <TableHead>Rule</TableHead>
                                    <TableHead>Last 5m / 1h / 1d / 1M</TableHead>
                                    <TableHead>Last alert</TableHead>
                                    <TableHead>Last message</TableHead>
                                    <TableHead>Severity</TableHead>
                                    <TableHead>Enabled</TableHead>
                                    {showActions && <TableHead className="w-12 text-right">Actions</TableHead>}
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {alerts.map(alert => (
                                    <TableRow key={alert.id}>
                                        <TableCell>
                                            <p className="text-sm font-medium">{alert.name}</p>
                                        </TableCell>
                                        <TableCell>
                                            <span className="text-sm text-muted-foreground">
                                                {getAlertRuleLabel(alert.source, alert.type)}
                                            </span>
                                        </TableCell>
                                        <TableCell>
                                            <AlertCountersCell counters={alert.counters} />
                                        </TableCell>
                                        <TableCell>
                                            <span className="text-sm text-muted-foreground">{formatLastAlertAt(alert.last_alert_at)}</span>
                                        </TableCell>
                                        <TableCell>
                                            <span className="text-sm text-muted-foreground line-clamp-1">
                                                {formatLastAlertMessage(alert.last_alert_message)}
                                            </span>
                                        </TableCell>
                                        <TableCell>
                                            <SeverityBadge severity={alert.severity} />
                                        </TableCell>
                                        <TableCell>
                                            <Switch
                                                checked={alert.enabled}
                                                disabled={
                                                    !canUpdate ||
                                                    !!alert.template ||
                                                    (toggleMutation.isPending && toggleMutation.variables?.id === alert.id)
                                                }
                                                onCheckedChange={() => toggleMutation.mutate(alert)}
                                            />
                                        </TableCell>
                                        {showActions && (
                                            <TableCell>
                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-8"
                                                            aria-label={`Actions for ${alert.name}`}
                                                        >
                                                            <MoreHorizontalIcon className="size-4" />
                                                        </Button>
                                                    </DropdownMenuTrigger>
                                                    <DropdownMenuContent align="end">
                                                        {canUpdate && alert.id && (
                                                            <DropdownMenuItem onClick={() => handleEdit(alert.id!)}>Edit</DropdownMenuItem>
                                                        )}
                                                        {canUpdate && canDelete && <DropdownMenuSeparator />}
                                                        {canDelete && (
                                                            <DropdownMenuItem
                                                                className="text-destructive focus:text-destructive"
                                                                onClick={() => setAlertToDelete(alert)}
                                                            >
                                                                Delete alert
                                                            </DropdownMenuItem>
                                                        )}
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            </TableCell>
                                        )}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TooltipProvider>
                </div>
            )}

            {alertToDelete ? (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && !deleteMutation.isPending && setAlertToDelete(null)}
                    title="Delete alert"
                    description={
                        <>
                            Are you sure you want to delete the alert <strong>{alertToDelete.name}</strong>?
                        </>
                    }
                    confirmLabel="Delete"
                    pendingLabel="Deleting…"
                    destructive
                    isPending={deleteMutation.isPending}
                    onConfirm={() => deleteMutation.mutate(alertToDelete)}
                />
            ) : null}
        </div>
    );
}
