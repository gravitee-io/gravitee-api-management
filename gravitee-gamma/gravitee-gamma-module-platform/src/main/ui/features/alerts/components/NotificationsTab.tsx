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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { PlusIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import type { Dispatch, SetStateAction } from 'react';

import { NotificationSchemaFields } from './NotificationSchemaFields';
import { DAMPENING_MODES, TIME_UNITS } from '../constants/alertConstants';
import type { AlertFormData } from '../services/alerts';
import { listNotifiers, type NotifierListItem } from '../services/notifiers';
import type { AlertDampeningMode, AlertFormNotification, AlertTimeUnit } from '../types';
import { platformAlertKeys } from '../utils/queryKeys';

const CHANNEL_ORDER = ['email-notifier', 'slack-notifier', 'default-email', 'webhook-notifier'];

function sortNotifiers(items: NotifierListItem[]): NotifierListItem[] {
    return [...items].sort((a, b) => {
        const ia = CHANNEL_ORDER.indexOf(a.id);
        const ib = CHANNEL_ORDER.indexOf(b.id);
        if (ia === -1 && ib === -1) return a.name.localeCompare(b.name);
        if (ia === -1) return 1;
        if (ib === -1) return -1;
        return ia - ib;
    });
}

export interface NotificationsTabProps {
    dampening: AlertFormData['dampening'];
    setDampening: Dispatch<SetStateAction<AlertFormData['dampening']>>;
    notifications: AlertFormNotification[];
    addNotification: () => void;
    removeNotification: (index: number) => void;
    setNotificationType: (index: number, type: string) => void;
    updateNotification: (index: number, configuration: Record<string, unknown>) => void;
    canEdit: boolean;
    markDirty: () => void;
    channelError?: string;
    dampeningError?: string;
}

export function NotificationsTab({
    dampening,
    setDampening,
    notifications,
    addNotification,
    removeNotification,
    setNotificationType,
    updateNotification,
    canEdit,
    markDirty,
    channelError,
    dampeningError,
}: NotificationsTabProps) {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const { data: notifierList, isError: notifierListFailed } = useQuery({
        queryKey: platformAlertKeys.notifiers(environmentId),
        queryFn: () => listNotifiers(environmentId),
        enabled: !!environmentId,
    });
    const notifiers = sortNotifiers(notifierList ?? []);

    return (
        <div className="mt-6 space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Dampening</CardTitle>
                    <CardDescription>
                        Allows you to limit the number of notifications if the trigger is fired multiple times for the same condition
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    {dampeningError && <p className="text-xs text-destructive">{dampeningError}</p>}
                    <div className="space-y-1.5">
                        <Label className="text-xs">
                            Mode <span className="text-destructive">*</span>
                        </Label>
                        <Select
                            value={dampening?.mode || 'STRICT_COUNT'}
                            disabled={!canEdit}
                            onValueChange={val => {
                                const mode = val as AlertDampeningMode;
                                const next = { mode } as AlertFormData['dampening'];
                                if (mode === 'STRICT_COUNT') next!.trueEvaluations = 1;
                                if (mode === 'RELAXED_COUNT') {
                                    next!.trueEvaluations = 1;
                                }
                                if (mode === 'RELAXED_TIME') {
                                    next!.trueEvaluations = 1;
                                    next!.timeUnit = 'MINUTES';
                                }
                                if (mode === 'STRICT_TIME') {
                                    next!.timeUnit = 'MINUTES';
                                }
                                setDampening(next);
                                markDirty();
                            }}
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {DAMPENING_MODES.map(dm => (
                                    <SelectItem key={dm.value} value={dm.value}>
                                        {dm.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <p className="text-xs text-muted-foreground">Select the most appropriate dampening mode for this alert.</p>
                    </div>

                    {(dampening?.mode === 'STRICT_COUNT' || dampening?.mode === 'RELAXED_COUNT' || dampening?.mode === 'RELAXED_TIME') && (
                        <div className="space-y-1.5">
                            <Label className="text-xs">
                                Number of true evaluations <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                type="number"
                                min={1}
                                max={100}
                                disabled={!canEdit}
                                value={dampening?.trueEvaluations ?? ''}
                                onChange={e => {
                                    setDampening(d => ({
                                        ...d!,
                                        trueEvaluations: e.target.value ? Number(e.target.value) : undefined,
                                    }));
                                    markDirty();
                                }}
                            />
                            <p className="text-xs text-muted-foreground">The number of consecutive true evaluations.</p>
                        </div>
                    )}

                    {dampening?.mode === 'RELAXED_COUNT' && (
                        <div className="space-y-1.5">
                            <Label className="text-xs">
                                Number of total evaluations <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                type="number"
                                min={dampening?.trueEvaluations ?? 1}
                                max={100}
                                disabled={!canEdit}
                                value={dampening?.totalEvaluations ?? ''}
                                aria-invalid={
                                    typeof dampening?.trueEvaluations === 'number' &&
                                    typeof dampening?.totalEvaluations === 'number' &&
                                    dampening.totalEvaluations < dampening.trueEvaluations
                                }
                                onChange={e => {
                                    setDampening(d => ({
                                        ...d!,
                                        totalEvaluations: e.target.value ? Number(e.target.value) : undefined,
                                    }));
                                    markDirty();
                                }}
                            />
                            {typeof dampening?.trueEvaluations === 'number' &&
                            typeof dampening?.totalEvaluations === 'number' &&
                            dampening.totalEvaluations < dampening.trueEvaluations ? (
                                <p className="text-xs text-destructive">
                                    Number of total evaluations must be at least as high as the number of true evaluations.
                                </p>
                            ) : (
                                <p className="text-xs text-muted-foreground">The number of total evaluations.</p>
                            )}
                        </div>
                    )}

                    {(dampening?.mode === 'STRICT_TIME' || dampening?.mode === 'RELAXED_TIME') && (
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1.5">
                                <Label className="text-xs">
                                    Duration <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    type="number"
                                    min={1}
                                    max={100}
                                    disabled={!canEdit}
                                    value={dampening?.duration ?? ''}
                                    onChange={e => {
                                        setDampening(d => ({
                                            ...d!,
                                            duration: e.target.value ? Number(e.target.value) : undefined,
                                        }));
                                        markDirty();
                                    }}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label className="text-xs">Time unit</Label>
                                <Select
                                    value={dampening?.timeUnit || 'MINUTES'}
                                    disabled={!canEdit}
                                    onValueChange={val => {
                                        setDampening(d => ({ ...d!, timeUnit: val as AlertTimeUnit }));
                                        markDirty();
                                    }}
                                >
                                    <SelectTrigger>
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {TIME_UNITS.map(tu => (
                                            <SelectItem key={tu.value} value={tu.value}>
                                                {tu.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                    )}
                </CardContent>
            </Card>

            <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                    <div className="space-y-1.5">
                        <CardTitle className="text-base">Notifications</CardTitle>
                        <CardDescription>Allows you to receive notifications via email, Slack, or webhooks.</CardDescription>
                    </div>
                    {canEdit ? (
                        <Button type="button" size="sm" className="shrink-0" onClick={addNotification}>
                            <PlusIcon className="size-4" aria-hidden />
                            Add
                        </Button>
                    ) : null}
                </CardHeader>
                {channelError || notifierListFailed || notifications.length > 0 ? (
                    <CardContent className="space-y-4">
                        {channelError && <p className="text-xs text-destructive">{channelError}</p>}
                        {notifierListFailed && (
                            <p className="text-xs text-destructive">Failed to load notification channels. Please try again.</p>
                        )}
                        {notifications.map((notif, idx) => (
                            <div key={`${idx}-${notif.type || 'unset'}`} className="relative space-y-4 rounded-lg border p-4">
                                {canEdit ? (
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="absolute top-2 right-2 size-7"
                                        aria-label="Remove notification"
                                        onClick={() => removeNotification(idx)}
                                    >
                                        <XIcon className="size-3.5 text-muted-foreground" />
                                    </Button>
                                ) : null}
                                <div className="space-y-1.5 pr-8">
                                    <Label className="text-xs">
                                        Channel <span className="text-destructive">*</span>
                                    </Label>
                                    <Select
                                        value={notif.type || undefined}
                                        disabled={!canEdit}
                                        onValueChange={val => setNotificationType(idx, val)}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="Channel" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {notifiers.map(notifier => (
                                                <SelectItem key={notifier.id} value={notifier.id}>
                                                    {notifier.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                {notif.type ? (
                                    <NotificationSchemaFields
                                        environmentId={environmentId}
                                        notifierId={notif.type}
                                        value={notif.configuration}
                                        disabled={!canEdit}
                                        onChange={configuration => updateNotification(idx, configuration)}
                                    />
                                ) : null}
                            </div>
                        ))}
                    </CardContent>
                ) : null}
            </Card>
        </div>
    );
}
