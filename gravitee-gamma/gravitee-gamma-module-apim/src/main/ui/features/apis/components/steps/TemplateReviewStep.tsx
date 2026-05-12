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
import { Badge, Button, Card, Switch } from '@gravitee/graphene-core';
import { FileTextIcon, GlobeIcon, RocketIcon, ScrollTextIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

import { SecureStep } from './SecureStep';
import type { ApiCreationState } from '../../types/models';
import { formatSecurityType } from '../../utils/securityFormatters';

type TemplateReviewStepProps = Readonly<{
    state: ApiCreationState;
    templateLabel: string;
    onEditEssentials: () => void;
    onChangeDeployImmediately: (next: boolean) => void;
    errors: Record<string, string>;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
}>;

export function TemplateReviewStep({
    state,
    templateLabel,
    onEditEssentials,
    onChangeDeployImmediately,
    errors,
    getValue,
    updateField,
}: TemplateReviewStepProps) {
    const [customizeOpen, setCustomizeOpen] = useState(false);

    const hasSecurityErrors = Object.keys(errors).some(k => k.startsWith('security.'));
    useEffect(() => {
        if (hasSecurityErrors) setCustomizeOpen(true);
    }, [hasSecurityErrors]);

    const gatewayUrl = state.proxy.enableVirtualHosts
        ? `https://${state.proxy.virtualHosts?.[0]?.host?.trim() || 'your-domain.example.com'}${state.proxy.virtualHosts?.[0]?.path?.trim() || '/'}`
        : `https://gateway.company.com${state.proxy.contextPath?.trim() || '/...'}`;

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="space-y-1">
                <div className="flex items-center gap-2">
                    <ScrollTextIcon className="size-4 text-primary" aria-hidden="true" />
                    <div className="text-sm font-semibold">Review Configuration</div>
                </div>
                <div className="text-xs text-muted-foreground">
                    Pre-configured by {templateLabel}. Expand Customize to override template defaults.
                </div>
            </div>

            {/* API Details */}
            <Card className="rounded-xl p-4 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                        <FileTextIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                        <div className="text-sm font-semibold">API Details</div>
                    </div>
                    <Button type="button" variant="link" size="sm" onClick={onEditEssentials}>
                        Edit
                    </Button>
                </div>
                <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
                    <div>
                        <div className="text-xs text-muted-foreground">Name</div>
                        <div className="font-medium">{state.details.name || '—'}</div>
                    </div>
                    <div>
                        <div className="text-xs text-muted-foreground">Version</div>
                        <div className="font-medium">{state.details.version || '—'}</div>
                    </div>
                    <div>
                        <div className="text-xs text-muted-foreground">Protocol</div>
                        <div className="font-medium">REST</div>
                    </div>
                    {state.details.description ? (
                        <div className="sm:col-span-3">
                            <div className="text-xs text-muted-foreground">Description</div>
                            <div className="font-medium">{state.details.description}</div>
                        </div>
                    ) : null}
                </div>
            </Card>

            {/* Proxy Configuration */}
            <Card className="rounded-xl p-4 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                        <GlobeIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                        <div className="text-sm font-semibold">Proxy Configuration</div>
                    </div>
                    <Button type="button" variant="link" size="sm" onClick={onEditEssentials}>
                        Edit
                    </Button>
                </div>
                <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                    <div>
                        <div className="text-xs text-muted-foreground">Gateway URL</div>
                        <div className="break-all font-medium font-mono text-xs">{gatewayUrl}</div>
                    </div>
                    <div>
                        <div className="text-xs text-muted-foreground">Upstream URL</div>
                        <div className="break-all font-medium font-mono text-xs">{state.proxy.targetUrl || '—'}</div>
                    </div>
                    <div>
                        <div className="text-xs text-muted-foreground">Virtual hosts</div>
                        <div className="font-medium">
                            {state.proxy.enableVirtualHosts ? `Enabled (${state.proxy.virtualHosts?.length ?? 0})` : 'Disabled'}
                        </div>
                    </div>
                </div>
            </Card>

            {/* Security — summary always visible; Customize expands full SecureStep */}
            <Card className="rounded-xl p-4 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                        <ShieldIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                        <div className="text-sm font-semibold">Security</div>
                    </div>
                    <button
                        type="button"
                        className="flex items-center gap-1 text-xs font-medium text-primary hover:underline focus-visible:outline-none"
                        onClick={() => setCustomizeOpen(o => !o)}
                    >
                        Customize {customizeOpen ? '▲' : '▼'}
                    </button>
                </div>

                {/* Quick-glance summary — always shown */}
                <div className="mt-3 flex flex-wrap items-center gap-2">
                    <Badge variant="secondary" className="border border-primary/20 bg-primary/10 text-primary text-xs">
                        {formatSecurityType(state.security.type)}
                    </Badge>
                    {'planName' in state.security && state.security.planName ? (
                        <Badge variant="secondary" className="text-xs">
                            {state.security.planName}
                        </Badge>
                    ) : null}
                </div>

                {/* Full interactive security form when Customize is open */}
                {customizeOpen ? (
                    <div className="mt-5 border-t pt-5">
                        <SecureStep security={state.security} errors={errors} getValue={getValue} updateField={updateField} />
                    </div>
                ) : null}
            </Card>

            {/* Deploy toggle */}
            <Card className="rounded-xl border border-success/25 bg-success/5 p-4 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                        <div className="flex items-center gap-2">
                            <RocketIcon className="size-4 text-success" aria-hidden="true" />
                            <div className="text-sm font-semibold">Deploy and start API immediately</div>
                        </div>
                        <div className="text-xs text-muted-foreground">
                            {state.deployImmediately
                                ? 'Enabled — proxy will start accepting traffic right away.'
                                : 'Disabled — you can deploy later.'}
                        </div>
                    </div>
                    <Switch
                        checked={state.deployImmediately}
                        aria-label="Deploy and start API immediately"
                        onCheckedChange={onChangeDeployImmediately}
                    />
                </div>
            </Card>
        </div>
    );
}
