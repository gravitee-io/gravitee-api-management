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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { usePortalsNavigation } from '../config/navigation';
import {
    getPortalDashboardStats,
    type PortalSparklineTone,
} from '../storage/dummy-dashboard-stats';
import type { DeveloperPortal } from '../types';
import {
    formatRelativeUpdatedAt,
    getPortalCustomDomain,
    getPortalPublishStatus,
} from '../utils/portal-display';
import { PortalStatusBadge } from './PortalStatusBadge';

function formatRequestsPerDay(value: number): string {
    if (value === 0) {
        return '0';
    }
    if (value >= 1000) {
        const thousands = value / 1000;
        return `${thousands % 1 === 0 ? thousands.toFixed(0) : thousands.toFixed(1)}k`;
    }
    return value.toLocaleString();
}

function sparklineStroke(tone: PortalSparklineTone): string {
    if (tone === 'positive') {
        return 'var(--success, #16a34a)';
    }
    if (tone === 'neutral') {
        return 'var(--warning, #ea580c)';
    }
    return 'var(--muted-foreground, #94a3b8)';
}

function PortalSparkline({
    values,
    tone,
}: {
    readonly values: readonly number[];
    readonly tone: PortalSparklineTone;
}) {
    const path = useMemo(() => {
        if (values.length === 0) {
            return '';
        }
        const width = 72;
        const height = 28;
        const max = Math.max(...values, 1);
        const min = Math.min(...values, 0);
        const range = Math.max(max - min, 1);
        return values
            .map((value, index) => {
                const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width;
                const y = height - ((value - min) / range) * (height - 4) - 2;
                return `${index === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
            })
            .join(' ');
    }, [values]);

    return (
        <svg
            width="72"
            height="28"
            viewBox="0 0 72 28"
            className="shrink-0"
            aria-hidden="true"
            focusable="false"
        >
            <path d={path} fill="none" stroke={sparklineStroke(tone)} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

function InfoRow({
    label,
    value,
    valueClassName,
}: {
    readonly label: string;
    readonly value: string;
    readonly valueClassName?: string;
}) {
    return (
        <div className="flex items-center justify-between gap-3 text-sm">
            <span className="text-muted-foreground">{label}</span>
            <span className={valueClassName ?? 'truncate text-right font-medium'}>{value}</span>
        </div>
    );
}

export function PortalCard({ portal }: { readonly portal: DeveloperPortal }) {
    const navigate = useNavigate();
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();
    const { portalSettingsSectionPath } = usePortalsNavigation();
    const stats = getPortalDashboardStats(portal.name);
    const status = getPortalPublishStatus(portal);
    const customDomain = getPortalCustomDomain(portal);
    const customDomainLabel = customDomain === '-' ? '—' : customDomain;
    const settingsPath = portalSettingsSectionPath(portal.id, 'general');
    const viewPath = `/portals/${portal.id}`;

    const handleConfigure = useCallback(() => {
        navigate(settingsPath);
    }, [navigate, settingsPath]);

    const handleViewPortal = useCallback(() => {
        // POC: ignore portalUrl (custom domains aren't wired; opening them hits DNS errors).
        if (embeddedInConsole) {
            window.open(buildStandalonePortalUrl(standaloneEditorBaseUrl, viewPath), '_blank', 'noopener,noreferrer');
            return;
        }
        navigate(viewPath);
    }, [embeddedInConsole, navigate, standaloneEditorBaseUrl, viewPath]);

    const latencyLabel = stats.avgLatencyMs === null ? '—' : `${stats.avgLatencyMs} ms`;

    return (
        <Card className="flex h-full flex-col">
            <CardContent className="flex h-full flex-col gap-4 pt-5 pb-4">
                <div className="flex items-start justify-between gap-3">
                    <h3 className="min-w-0 truncate text-base font-semibold leading-snug">{portal.name}</h3>
                    <PortalStatusBadge status={status} />
                </div>

                <div className="flex items-end justify-between gap-3">
                    <div>
                        <p className="text-3xl font-semibold tracking-tight">
                            {formatRequestsPerDay(stats.requestsPerDay)}
                        </p>
                        <p className="text-sm text-muted-foreground">Requests / day</p>
                    </div>
                    <PortalSparkline values={stats.sparkline} tone={stats.sparklineTone} />
                </div>

                <div className="space-y-2 border-t pt-3">
                    <InfoRow label="Custom Domain" value={customDomainLabel} />
                    <InfoRow label="Last Updated" value={formatRelativeUpdatedAt(portal.updatedAt)} />
                    <InfoRow
                        label="Avg. Latency"
                        value={latencyLabel}
                        valueClassName={
                            stats.avgLatencyMs === null
                                ? 'truncate text-right font-medium text-muted-foreground'
                                : 'truncate text-right font-medium text-success'
                        }
                    />
                </div>

                <div className="mt-auto flex items-center gap-2 pt-1">
                    <Button type="button" variant="outline" size="sm" className="flex-1" onClick={handleConfigure}>
                        Configure
                    </Button>
                    <Button type="button" variant="outline" size="sm" className="flex-1" onClick={handleViewPortal}>
                        View Portal
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}
