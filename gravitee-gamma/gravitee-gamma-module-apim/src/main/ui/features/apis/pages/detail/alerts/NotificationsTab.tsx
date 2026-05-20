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
import {
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
} from '@gravitee/graphene-core';
import { MailIcon, MessageSquareIcon, PlusIcon, WebhookIcon } from '@gravitee/graphene-core/icons';
import type { Dispatch, SetStateAction } from 'react';

import { DAMPENING_MODES, NOTIFICATION_CHANNELS, TIME_UNITS } from '../../../constants/alertConstants';
import type { AlertFormData } from '../../../services/alerts';
import type { AlertDampeningMode, AlertFormNotification, AlertNotificationChannel, AlertTimeUnit } from '../../../types/api';

const CHANNEL_META: Record<
    AlertNotificationChannel,
    { label: string; icon: React.ComponentType<{ className?: string }>; placeholder: string; inputLabel: string }
> = {
    'email-notifier': { label: 'E-mail', icon: MailIcon, placeholder: 'ops@company.com', inputLabel: 'Recipient email' },
    'default-email': { label: 'System e-mail', icon: MailIcon, placeholder: 'ops@company.com', inputLabel: 'Recipient email' },
    'slack-notifier': {
        label: 'Slack',
        icon: MessageSquareIcon,
        placeholder: 'https://hooks.slack.com/services/…',
        inputLabel: 'Slack webhook URL',
    },
    'webhook-notifier': { label: 'Webhook', icon: WebhookIcon, placeholder: 'https://hooks.example.com/…', inputLabel: 'Webhook URL' },
};

export interface NotificationsTabProps {
    dampening: AlertFormData['dampening'];
    setDampening: Dispatch<SetStateAction<AlertFormData['dampening']>>;
    notifications: AlertFormNotification[];
    addNotification: (channel: AlertNotificationChannel) => void;
    removeNotification: (index: number) => void;
    updateNotificationTarget: (index: number, target: string) => void;
    canEdit: boolean;
    markDirty: () => void;
}

export function NotificationsTab({
    dampening,
    setDampening,
    notifications,
    addNotification,
    removeNotification,
    updateNotificationTarget,
    canEdit,
    markDirty,
}: NotificationsTabProps) {
    return (
        <div className="mt-6 space-y-6">
            {/* Dampening */}
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Dampening</CardTitle>
                    <CardDescription>
                        Allows you to limit the number of notifications if the trigger is fired multiple times for the same condition
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-1.5">
                        <Label className="text-xs">Mode</Label>
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
                            <Label className="text-xs">Number of true evaluations</Label>
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
                            <p className="text-xs text-muted-foreground">The number from 1 to 100 of consecutive true evaluations.</p>
                        </div>
                    )}

                    {dampening?.mode === 'RELAXED_COUNT' && (
                        <div className="space-y-1.5">
                            <Label className="text-xs">Number of total evaluations</Label>
                            <Input
                                type="number"
                                disabled={!canEdit}
                                value={dampening?.totalEvaluations ?? ''}
                                onChange={e => {
                                    setDampening(d => ({
                                        ...d!,
                                        totalEvaluations: e.target.value ? Number(e.target.value) : undefined,
                                    }));
                                    markDirty();
                                }}
                            />
                            <p className="text-xs text-muted-foreground">The number of total evaluations.</p>
                        </div>
                    )}

                    {(dampening?.mode === 'STRICT_TIME' || dampening?.mode === 'RELAXED_TIME') && (
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1.5">
                                <Label className="text-xs">Duration</Label>
                                <Input
                                    type="number"
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

            {/* Notifications */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <div>
                            <CardTitle className="text-base">Notifications</CardTitle>
                            <CardDescription>Allow you to receive notifications via email, Slack or webhooks.</CardDescription>
                        </div>
                        {canEdit && (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button size="sm">
                                        <PlusIcon className="size-4" />
                                        Add notification
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                    {NOTIFICATION_CHANNELS.map(ch => (
                                        <DropdownMenuItem key={ch.value} onSelect={() => addNotification(ch.value)}>
                                            {ch.label}
                                        </DropdownMenuItem>
                                    ))}
                                </DropdownMenuContent>
                            </DropdownMenu>
                        )}
                    </div>
                </CardHeader>
                {notifications.length > 0 && (
                    <CardContent className="space-y-4 pt-0">
                        {notifications.map((notif, idx) => {
                            const meta = CHANNEL_META[notif.channel];
                            const ChIcon = meta.icon;
                            return (
                                <div key={idx}>
                                    <Separator className="mb-4" />
                                    <div className="space-y-3">
                                        <div className="flex items-center justify-between">
                                            <h4 className="flex items-center gap-2 text-sm font-medium">
                                                <ChIcon className="size-4 text-muted-foreground" />
                                                Configure {meta.label} notification
                                            </h4>
                                            {canEdit && (
                                                <Button variant="outline" size="sm" onClick={() => removeNotification(idx)}>
                                                    Remove
                                                </Button>
                                            )}
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label className="text-xs">{meta.inputLabel}</Label>
                                            <Input
                                                placeholder={meta.placeholder}
                                                value={notif.target}
                                                disabled={!canEdit}
                                                onChange={e => updateNotificationTarget(idx, e.target.value)}
                                            />
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </CardContent>
                )}
            </Card>
        </div>
    );
}
