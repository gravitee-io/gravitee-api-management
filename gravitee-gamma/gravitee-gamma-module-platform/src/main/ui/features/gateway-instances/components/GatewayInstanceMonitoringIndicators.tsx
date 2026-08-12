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
import type { ReactNode } from 'react';

import type { MonitoringData } from '../types/instance';

function Row({ label, value, divider = true }: { label: string; value: string | number; divider?: boolean }) {
    return (
        <div className={['flex items-baseline justify-between gap-4 py-3 text-sm', divider ? 'border-b border-border' : ''].join(' ')}>
            <span className="text-muted-foreground">{label}</span>
            <span className="shrink-0 text-right font-medium">{value}</span>
        </div>
    );
}

function SectionCard({ title, testId, children }: { title: string; testId: string; children: ReactNode }) {
    return (
        <Card data-testid={testId}>
            <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-base font-semibold">
                    <SettingsIcon className="size-4 text-muted-foreground" aria-hidden />
                    {title}
                </CardTitle>
            </CardHeader>
            <CardContent>{children}</CardContent>
        </Card>
    );
}

export function GatewayInstanceMonitoringIndicators({ data }: Readonly<{ data: MonitoringData }>) {
    const { cpu, process, thread, gc } = data;

    return (
        <div className="grid gap-4 md:grid-cols-2">
            <SectionCard title="CPU" testId="instance-monitoring_cpu-box">
                <Row label="Percent of use" value={`${cpu.percent_use}%`} />
                <div className="py-3">
                    <div className="mb-2 text-sm text-muted-foreground">Load average</div>
                    <div className="space-y-2">
                        {Object.entries(cpu.load_average ?? {}).map(([key, value]) => (
                            <div key={key} className="flex items-baseline justify-between gap-4 text-sm">
                                <span className="text-muted-foreground">[{key}]</span>
                                <span className="shrink-0 text-right font-medium">{value}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </SectionCard>
            <SectionCard title="Process" testId="instance-monitoring_process-box">
                <Row label="Open file descriptors" value={process.open_file_descriptors} />
                <Row label="Max file descriptors" value={process.max_file_descriptors} divider={false} />
            </SectionCard>
            <SectionCard title="Thread" testId="instance-monitoring_thread-box">
                <Row label="Count" value={thread.count} />
                <Row label="Peak count" value={thread.peak_count} divider={false} />
            </SectionCard>
            <SectionCard title="Garbage collector" testId="instance-monitoring_gc-box">
                <Row label="Young collection count" value={gc.young_collection_count} />
                <Row label="Young collection time" value={`${gc.young_collection_time_in_millis} ms`} />
                <Row label="Old collection count" value={gc.old_collection_count} />
                <Row label="Old collection time" value={`${gc.old_collection_time_in_millis} ms`} divider={false} />
            </SectionCard>
        </div>
    );
}
