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
import { ArrowRightIcon, ZapIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';
import { Link } from 'react-router-dom';

import { type Application } from './applications';
import { UpgradeDialog } from './UpgradeDialog';

export interface MetricLine {
    readonly value: number | null;
    readonly label: string;
}

export interface CardMetrics {
    readonly primary: MetricLine;
    readonly secondary?: MetricLine;
    /** True while the count is still being fetched. Once settled, a `null` value means "no data". */
    readonly loading: boolean;
}

const HOVER_RING = `0 0 0 1px color-mix(in oklab, var(--color-muted-foreground) 40%, transparent), 0 4px 16px 0 rgb(0 0 0 / 0.08)`;

function formatCompact(n: number): string {
    if (n >= 10_000) return `${(n / 1000).toFixed(1).replace(/\.0$/, '')}k`;
    return n.toLocaleString('en-US');
}

function MetricsSkeleton() {
    return (
        <div className="space-y-1.5 pt-1">
            <span className="inline-block h-5 w-16 animate-pulse rounded bg-muted" />
            <span className="inline-block h-3 w-24 animate-pulse rounded bg-muted" />
        </div>
    );
}

/**
 * Four visual states:
 *  - **Metrics view** (`to !== null` and has data): live stats + "Open" CTA.
 *  - **Empty state** (`to !== null` but no data): description + module-specific CTA.
 *  - **Locked** (`to === null`): description + "Upgrade to access" CTA opening the upgrade dialog.
 *    A module is locked when it is absent from `/modules`, i.e. not covered by the organization license.
 */
export function ApplicationCard({
    app,
    to,
    metrics,
}: {
    readonly app: Application;
    readonly to: string | null;
    readonly metrics?: CardMetrics;
}) {
    const { Icon, title, description, emptyState } = app;
    const titleId = useId();
    const [isHovered, setIsHovered] = useState(false);
    const [upgradeOpen, setUpgradeOpen] = useState(false);

    const isLoading = metrics?.loading === true;
    const hasData = metrics?.primary.value !== null && metrics?.primary.value !== undefined && metrics.primary.value > 0;
    const isEmptyState = to !== null && !hasData && !isLoading;
    const ctaLabel = isEmptyState ? emptyState.cta : 'Open';
    const ctaTarget = isEmptyState && emptyState.ctaPath ? `${to}/${emptyState.ctaPath}` : to;

    const inner = (
        <CardContent className="flex h-full flex-col gap-3">
            <div className="flex size-12 items-center justify-center rounded-lg bg-muted">
                <Icon className="size-8" aria-hidden />
            </div>
            <h3 id={titleId} className="text-sm font-semibold leading-tight">
                {title}
            </h3>
            {hasData ? (
                <div className="-mt-1 space-y-0.5 border-t border-border/50 pt-3">
                    <p className="text-2xl font-bold tracking-tight">
                        {formatCompact(metrics.primary.value!)}{' '}
                        <span className="text-xs font-normal text-muted-foreground">{metrics.primary.label}</span>
                    </p>
                    {metrics.secondary?.value !== null && metrics.secondary?.value !== undefined && (
                        <p className="text-xs text-muted-foreground">
                            {formatCompact(metrics.secondary.value)} {metrics.secondary.label}
                        </p>
                    )}
                </div>
            ) : isLoading ? (
                <MetricsSkeleton />
            ) : (
                <p className="text-xs text-muted-foreground">{description}</p>
            )}
            {ctaTarget !== null && (
                <p
                    className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors duration-150"
                    style={{ color: isHovered ? 'var(--color-foreground)' : undefined }}
                >
                    {ctaLabel}
                    <ArrowRightIcon
                        className="size-3 transition-transform duration-150"
                        aria-hidden
                        style={{ transform: isHovered ? 'translateX(3px)' : undefined }}
                    />
                </p>
            )}
            {to === null && app.upgrade && (
                <Button
                    variant="outline"
                    size="sm"
                    className="mt-auto self-start rounded-full border-primary px-4 text-primary hover:bg-primary/10 hover:text-primary"
                    onClick={() => setUpgradeOpen(true)}
                >
                    <ZapIcon aria-hidden />
                    Upgrade to access
                </Button>
            )}
        </CardContent>
    );

    if (to === null) {
        return (
            <>
                <Card role="group" aria-labelledby={titleId} className="h-full">
                    {inner}
                </Card>
                {app.upgrade && <UpgradeDialog app={app} open={upgradeOpen} onOpenChange={setUpgradeOpen} />}
            </>
        );
    }

    return (
        <Link
            to={ctaTarget!}
            className="cursor-pointer rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            onFocus={() => setIsHovered(true)}
            onBlur={() => setIsHovered(false)}
        >
            <Card className="h-full transition-shadow duration-150" style={{ boxShadow: isHovered ? HOVER_RING : undefined }}>
                {inner}
            </Card>
        </Link>
    );
}
