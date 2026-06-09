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
import { Badge, Separator, Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@gravitee/graphene-core';

import type { HealthCheckLog, HealthCheckStep } from '../../../../types/healthCheck';
import { formatTimestamp } from '../../../../utils/healthCheckDashboard';

interface HealthCheckLogDetailSheetProps {
    log: HealthCheckLog | null;
    onClose: () => void;
}

export function HealthCheckLogDetailSheet({ log, onClose }: Readonly<HealthCheckLogDetailSheetProps>) {
    const open = log !== null;
    return (
        <Sheet
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) onClose();
            }}
        >
            <SheetContent side="right" className="max-w-[560px]">
                <SheetHeader>
                    <SheetTitle>Health check detail</SheetTitle>
                    <SheetDescription>Request and response captured during the failed health check.</SheetDescription>
                </SheetHeader>

                {log && (
                    <div className="overflow-y-auto flex-1 min-h-0 space-y-5 px-4">
                        <SummarySection log={log} />
                        {log.steps.map((step, index) => (
                            <StepSection key={`${step.name}-${index}`} step={step} />
                        ))}
                    </div>
                )}
            </SheetContent>
        </Sheet>
    );
}

function SummarySection({ log }: Readonly<{ log: HealthCheckLog }>) {
    return (
        <dl className="grid grid-cols-2 gap-3 text-sm">
            <Field label="Timestamp" value={formatTimestamp(log.timestamp)} />
            <Field
                label="Status"
                value={<Badge variant={log.success ? 'secondary' : 'destructive'}>{log.success ? 'Up' : 'Down'}</Badge>}
            />
            <Field label="Endpoint" value={log.endpointName} />
            <Field label="Gateway" value={log.gatewayId} />
            <Field label="Response time" value={`${log.responseTime} ms`} />
        </dl>
    );
}

function StepSection({ step }: Readonly<{ step: HealthCheckStep }>) {
    return (
        <div className="space-y-3 rounded-lg border p-4">
            <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-medium">{step.name || 'Step'}</p>
                <Badge variant={step.success ? 'secondary' : 'destructive'}>{step.success ? 'Success' : 'Failure'}</Badge>
            </div>
            {step.message && <p className="text-xs text-muted-foreground">{step.message}</p>}

            {step.request && (
                <>
                    <Separator />
                    <div className="space-y-2">
                        <p className="text-xs font-semibold text-muted-foreground uppercase">Request</p>
                        <p className="font-mono text-xs break-all">
                            {step.request.method} {step.request.uri}
                        </p>
                        <HeadersList headers={step.request.headers} />
                    </div>
                </>
            )}

            {step.response && (
                <>
                    <Separator />
                    <div className="space-y-2">
                        <p className="text-xs font-semibold text-muted-foreground uppercase">Response</p>
                        <p className="font-mono text-xs">Status: {step.response.status}</p>
                        <HeadersList headers={step.response.headers} />
                        {step.response.body && (
                            <pre className="max-h-48 overflow-auto rounded bg-muted p-2 font-mono text-xs whitespace-pre-wrap">
                                {step.response.body}
                            </pre>
                        )}
                    </div>
                </>
            )}
        </div>
    );
}

function HeadersList({ headers }: Readonly<{ headers: Record<string, string> }>) {
    const entries = Object.entries(headers ?? {});
    if (entries.length === 0) return null;
    return (
        <ul className="space-y-0.5">
            {entries.map(([name, value]) => (
                <li key={name} className="font-mono text-xs text-muted-foreground break-all">
                    <span className="text-foreground">{name}</span>: {value}
                </li>
            ))}
        </ul>
    );
}

function Field({ label, value }: Readonly<{ label: string; value: React.ReactNode }>) {
    return (
        <div className="min-w-0">
            <dt className="text-xs text-muted-foreground">{label}</dt>
            <dd className="font-medium break-all">{value}</dd>
        </div>
    );
}
