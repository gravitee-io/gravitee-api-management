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

import { Card, CardContent, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { SettingsIcon } from '@gravitee/graphene-core/icons';

import { GatewayInstanceMonitoringIndicators } from './GatewayInstanceMonitoringIndicators';
import { MonitoringCircularGauge } from './MonitoringCircularGauge';
import type { MonitoringData } from '../types/instance';
import { formatInstanceDate } from '../utils/formatInstanceDate';
import { humanizeSize, humanizeUptime, ratio, ratioLabel } from '../utils/monitoringFormatters';

function DescriptionRow({ label, value, divider = true }: { label: string; value: string | number; divider?: boolean }) {
    return (
        <div className={['flex items-baseline justify-between gap-4 py-3 text-sm', divider ? 'border-b border-border' : ''].join(' ')}>
            <span className="text-muted-foreground">{label}</span>
            <span className="shrink-0 text-right font-medium">{value}</span>
        </div>
    );
}

function ProgressBar({ value, label }: { value: number | undefined; label: string }) {
    const width = value === undefined ? 0 : Math.min(100, Math.max(0, value));
    return (
        <div style={{ padding: '12px 0' }} data-testid="monitoring-progress-bar">
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 6 }}>
                <strong style={{ fontSize: 14, fontWeight: 600 }}>{label}</strong>
            </div>
            <div
                role="progressbar"
                aria-valuenow={width}
                aria-valuemin={0}
                aria-valuemax={100}
                style={{
                    height: 8,
                    width: '100%',
                    borderRadius: 9999,
                    overflow: 'hidden',
                    backgroundColor: '#f5d0c5',
                }}
            >
                <div
                    style={{
                        height: '100%',
                        width: `${width}%`,
                        borderRadius: 9999,
                        backgroundColor: 'var(--primary)',
                        transition: 'width 300ms ease',
                    }}
                />
            </div>
        </div>
    );
}

function PoolCard({
    usedLabel,
    maxLabel,
    peakUsedLabel,
    peakMaxLabel,
    used,
    max,
    peakUsed,
    peakMax,
}: {
    usedLabel: string;
    maxLabel: string;
    peakUsedLabel: string;
    peakMaxLabel: string;
    used: number;
    max: number;
    peakUsed: number;
    peakMax: number;
}) {
    return (
        <Card>
            <CardContent className="pt-6">
                <DescriptionRow label={usedLabel} value={humanizeSize(used)} />
                <DescriptionRow label={maxLabel} value={humanizeSize(max)} divider={false} />
                <ProgressBar value={ratio(used, max)} label={ratioLabel(used, max)} />
                <DescriptionRow label={peakUsedLabel} value={humanizeSize(peakUsed)} />
                <DescriptionRow label={peakMaxLabel} value={humanizeSize(peakMax)} divider={false} />
                <ProgressBar value={ratio(peakUsed, peakMax)} label={ratioLabel(peakUsed, peakMax)} />
            </CardContent>
        </Card>
    );
}

function formatJvmTimestamp(timestamp: number | Date): string {
    if (timestamp instanceof Date) {
        if (Number.isNaN(timestamp.getTime())) {
            return '—';
        }
        return timestamp.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            second: '2-digit',
        });
    }
    return formatInstanceDate(timestamp);
}

function HeroValueCard({ value, label }: { value: number; label: string }) {
    return (
        <Card className="h-full">
            <CardContent className="flex h-full min-h-[10.5rem] flex-col items-center justify-end gap-3 py-6">
                <div className="font-semibold tabular-nums" style={{ color: '#1f77b4', fontSize: '1.875rem' }}>
                    {value}
                </div>
                <h4 className="text-sm font-medium text-muted-foreground">{label}</h4>
            </CardContent>
        </Card>
    );
}

export function GatewayInstanceMonitoringView({ data }: Readonly<{ data: MonitoringData }>) {
    const { process, jvm, gc } = data;

    return (
        <div className="space-y-6" data-testid="gateway-instance-monitoring">
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <Card className="h-full">
                    <CardContent className="flex h-full min-h-[10.5rem] flex-col items-center justify-end py-6">
                        <MonitoringCircularGauge pct={process.cpu_percent} label="CPU" />
                    </CardContent>
                </Card>
                <Card className="h-full">
                    <CardContent className="flex h-full min-h-[10.5rem] flex-col items-center justify-end py-6">
                        <MonitoringCircularGauge pct={jvm.heap_used_percent} label="Heap" />
                    </CardContent>
                </Card>
                <HeroValueCard value={gc.old_collection_count} label="GC collections" />
                <HeroValueCard value={process.open_file_descriptors} label="File Descriptors" />
            </div>

            <Card data-testid="instance-monitoring_jvm-box">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base font-semibold">
                        <SettingsIcon className="size-4 text-muted-foreground" aria-hidden />
                        JVM
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div>
                        <DescriptionRow label="Date" value={formatJvmTimestamp(jvm.timestamp)} />
                        <DescriptionRow label="Uptime" value={humanizeUptime(jvm.uptime_in_millis)} />
                        <DescriptionRow label="Heap used" value={humanizeSize(jvm.heap_used_in_bytes)} />
                        <DescriptionRow label="Percent of heap used" value={`${jvm.heap_used_percent}%`} />
                        <DescriptionRow label="Heap committed" value={humanizeSize(jvm.heap_committed_in_bytes)} />
                        <DescriptionRow label="Heap max" value={humanizeSize(jvm.heap_max_in_bytes)} />
                        <DescriptionRow label="Non heap used" value={humanizeSize(jvm.non_heap_used_in_bytes)} />
                        <DescriptionRow label="Non heap committed" value={humanizeSize(jvm.non_heap_committed_in_bytes)} divider={false} />
                    </div>
                    <div className="grid gap-4 md:grid-cols-3">
                        <PoolCard
                            usedLabel="Young pool used"
                            maxLabel="Young pool max"
                            peakUsedLabel="Young pool peak used"
                            peakMaxLabel="Young pool peak max"
                            used={jvm.young_pool_used_in_bytes}
                            max={jvm.young_pool_max_in_bytes}
                            peakUsed={jvm.young_pool_peak_used_in_bytes}
                            peakMax={jvm.young_pool_peak_max_in_bytes}
                        />
                        <PoolCard
                            usedLabel="Survivor pool used"
                            maxLabel="Survivor pool max"
                            peakUsedLabel="Survivor pool peak used"
                            peakMaxLabel="Survivor pool peak max"
                            used={jvm.survivor_pool_used_in_bytes}
                            max={jvm.survivor_pool_max_in_bytes}
                            peakUsed={jvm.survivor_pool_peak_used_in_bytes}
                            peakMax={jvm.survivor_pool_peak_max_in_bytes}
                        />
                        <PoolCard
                            usedLabel="Old pool used"
                            maxLabel="Old pool max"
                            peakUsedLabel="Old pool peak used"
                            peakMaxLabel="Old pool peak max"
                            used={jvm.old_pool_used_in_bytes}
                            max={jvm.old_pool_max_in_bytes}
                            peakUsed={jvm.old_pool_peak_used_in_bytes}
                            peakMax={jvm.old_pool_peak_max_in_bytes}
                        />
                    </div>
                </CardContent>
            </Card>

            <GatewayInstanceMonitoringIndicators data={data} />
        </div>
    );
}
