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

import type { NotificationChannel, NotificationRow } from '../../features/apis/hooks/useApiNotifications';
import { CHANNEL_ICON } from '../../features/apis/utils/notificationFormatters';

// ─── Colour tokens for each channel (inline to stay MF-safe) ─────────────────

const CHANNEL_CONFIG: Record<NotificationChannel, { label: string; iconBg: string; iconCls: string }> = {
    CONSOLE: {
        label: 'Console notifiers',
        iconBg: 'color-mix(in oklab, var(--color-primary) 10%, transparent)',
        iconCls: 'size-5 text-primary',
    },
    EMAIL: {
        label: 'Email notifiers',
        iconBg: 'color-mix(in oklab, var(--color-success) 10%, transparent)',
        iconCls: 'size-5 text-success',
    },
    WEBHOOK: {
        label: 'Webhook notifiers',
        iconBg: 'color-mix(in oklab, var(--color-warning) 10%, transparent)',
        iconCls: 'size-5 text-warning',
    },
};

function SummaryCard({ channel, count, isLoading }: Readonly<{ channel: NotificationChannel; count: number; isLoading: boolean }>) {
    const { label, iconBg, iconCls } = CHANNEL_CONFIG[channel];
    const Icon = CHANNEL_ICON[channel];
    return (
        <Card className="flex-1">
            <CardContent className="p-5">
                <div className="flex items-center gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg" style={{ backgroundColor: iconBg }}>
                        <Icon className={iconCls} />
                    </div>
                    <div>
                        {isLoading ? (
                            <Skeleton className="h-7 w-10 rounded" />
                        ) : (
                            <p className="text-2xl font-semibold tracking-tight">{count}</p>
                        )}
                        <p className="text-xs text-muted-foreground">{label}</p>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}

interface NotificationsSummaryCardsProps {
    rows: NotificationRow[];
    isLoading: boolean;
}

export function NotificationsSummaryCards({ rows, isLoading }: Readonly<NotificationsSummaryCardsProps>) {
    const consoleCount = rows.filter(r => r.channel === 'CONSOLE').length;
    const emailCount = rows.filter(r => r.channel === 'EMAIL').length;
    const webhookCount = rows.filter(r => r.channel === 'WEBHOOK').length;

    return (
        <div className="flex gap-4">
            <SummaryCard channel="CONSOLE" count={consoleCount} isLoading={isLoading} />
            <SummaryCard channel="EMAIL" count={emailCount} isLoading={isLoading} />
            <SummaryCard channel="WEBHOOK" count={webhookCount} isLoading={isLoading} />
        </div>
    );
}
