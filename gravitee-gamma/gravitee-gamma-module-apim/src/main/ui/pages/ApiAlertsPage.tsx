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
import { Button, Card, cn } from '@gravitee/graphene-core';
import {
    ActivityIcon,
    ArrowRightIcon,
    BellIcon,
    CircleCheckIcon,
    MailIcon,
    MessageSquareIcon,
    PlusIcon,
    WebhookIcon,
} from '@gravitee/graphene-core/icons';

const DOCS_URL = 'https://documentation.gravitee.io/apim/guides/api-monitoring/alerts';

const ALERT_RULES = [
    { label: 'Alert when a metric of the request validates a condition', tag: 'API metrics' },
    { label: 'Alert when there is no request matching filters received for a period of time', tag: 'API metrics' },
    { label: 'Alert when the aggregated value of a request metric rises a threshold', tag: 'API metrics' },
    { label: 'Alert when the rate of a given condition rises a threshold', tag: 'API metrics' },
    { label: 'Alert when the health status of an endpoint has changed', tag: 'Health-check' },
] as const;

const CAPABILITIES = [
    'Monitor response times, error rates, and throughput',
    'Detect health check status changes on endpoints',
    'Send notifications via email, Slack, or webhooks',
    'Set severity levels: info, warning, or critical',
    'Configure timeframes for notification windows',
    'Apply filters to target specific traffic subsets',
] as const;

export function ApiAlertsPage() {
    return (
        <div className="space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Runtime Alerts</h1>
                    <p className="text-sm text-muted-foreground">Set up alerting conditions for the Gateway.</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    <Button type="button" variant="outline" size="sm" asChild>
                        <a href={DOCS_URL} target="_blank" rel="noopener noreferrer">
                            Documentation
                        </a>
                    </Button>
                    <Button type="button" size="sm" disabled>
                        <PlusIcon className="size-4" aria-hidden="true" />
                        Add alert
                    </Button>
                </div>
            </div>

            <Card className="p-6 space-y-6">
                {/* Why section */}
                <div className="space-y-1">
                    <p className="text-sm font-semibold">Why configure runtime alerts?</p>
                    <p className="text-sm text-muted-foreground">
                        Get notified automatically when API traffic anomalies occur — latency spikes, error rate increases, health check
                        failures — without constantly monitoring dashboards.
                    </p>
                </div>

                {/* How it works */}
                <div className="rounded-xl border border-primary/20 bg-primary/5 p-5">
                    <p className="mb-4 text-xs font-semibold text-primary">How it works</p>
                    <div className="flex items-center justify-center gap-3">
                        <FlowNode icon={ActivityIcon} label="Gateway traffic" />
                        <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden="true" />
                        <FlowNode icon={BellIcon} label="Alert rule" active />
                        <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden="true" />
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
                            <p className="text-[10px] text-muted-foreground">Email · Slack · Webhook</p>
                        </div>
                    </div>
                </div>

                {/* Two-column: rules + capabilities */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-3">
                        <p className="text-xs font-semibold">Available alert rules</p>
                        <ul className="space-y-2.5">
                            {ALERT_RULES.map(rule => (
                                <li key={rule.label} className="flex items-start gap-2">
                                    <BellIcon className="size-3.5 shrink-0 mt-0.5 text-primary" aria-hidden="true" />
                                    <span className="text-xs text-muted-foreground">
                                        {rule.label} <span className="text-muted-foreground/60">({rule.tag})</span>
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
                                    <CircleCheckIcon className="size-3.5 shrink-0 mt-0.5 text-success" aria-hidden="true" />
                                    <span className="text-xs text-muted-foreground">{cap}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
            </Card>
        </div>
    );
}

function FlowNode({
    icon: Icon,
    label,
    active = false,
}: Readonly<{ icon: React.ComponentType<{ className?: string }>; label: string; active?: boolean }>) {
    return (
        <div className="flex flex-col items-center gap-1.5">
            <div className={cn('rounded-lg p-2', active ? 'border-2 border-primary/30 bg-primary/10' : 'bg-muted')}>
                <Icon className={cn('size-4', active ? 'text-primary' : 'text-muted-foreground')} />
            </div>
            <p className="text-[10px] text-muted-foreground text-center">{label}</p>
        </div>
    );
}
