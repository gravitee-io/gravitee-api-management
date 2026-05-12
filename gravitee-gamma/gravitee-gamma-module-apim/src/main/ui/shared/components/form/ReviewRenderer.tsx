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
import { CircleCheckIcon, FileTextIcon, GlobeIcon, ScrollTextIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import type { ApiCreationState } from '../../../features/apis/types/models';
import { formatSecurityType } from '../../../features/apis/utils/securityFormatters';
import type { StepConfig } from '../../../utils/stepRegistry';

export type ReviewRendererProps = Readonly<{
    steps: readonly StepConfig[];
    state: ApiCreationState;
    onEditStep: (stepId: StepConfig['id']) => void;
    onChangeDeployImmediately: (next: boolean) => void;
}>;

export function ReviewRenderer({ steps, state, onEditStep, onChangeDeployImmediately }: ReviewRendererProps) {
    const sections = useMemo(() => {
        const byId = new Map(steps.map(step => [step.id, step] as const));
        const ordered = ['api-details', 'configure-proxy', 'secure'] as const;
        return ordered.map(id => byId.get(id)).filter(Boolean) as StepConfig[];
    }, [steps]);

    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <div className="flex items-center gap-2">
                    <ScrollTextIcon className="size-4 text-primary" aria-hidden="true" />
                    <div className="text-sm font-semibold">Review Configuration</div>
                </div>
                <div className="text-xs text-muted-foreground">Click Edit to jump back to any section.</div>
            </div>

            {sections.map(section => {
                const SectionIcon =
                    section.id === 'api-details'
                        ? FileTextIcon
                        : section.id === 'configure-proxy'
                          ? GlobeIcon
                          : section.id === 'secure'
                            ? ShieldIcon
                            : undefined;

                return (
                    <Card key={section.id} className="rounded-xl p-4 sm:p-5">
                        <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0">
                                <div className="flex items-center gap-2">
                                    {SectionIcon ? <SectionIcon className="size-4 text-muted-foreground" aria-hidden="true" /> : null}
                                    <div className="text-sm font-semibold">{section.label}</div>
                                </div>
                            </div>
                            <Button type="button" variant="link" size="sm" onClick={() => onEditStep(section.id)}>
                                Edit
                            </Button>
                        </div>

                        {section.id === 'api-details' ? (
                            <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
                                <div>
                                    <div className="text-xs text-muted-foreground">Name</div>
                                    <div className="font-medium">{state.details.name || '—'}</div>
                                </div>
                                <div>
                                    <div className="text-xs text-muted-foreground">Version</div>
                                    <div className="font-medium">{state.details.version || '—'}</div>
                                </div>
                                <div className="sm:col-span-3">
                                    <div className="text-xs text-muted-foreground">Description</div>
                                    <div className="font-medium">{state.details.description || '—'}</div>
                                </div>
                            </div>
                        ) : null}

                        {section.id === 'configure-proxy' ? (
                            <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                                <div>
                                    <div className="text-xs text-muted-foreground">Gateway URL</div>
                                    <div className="font-medium">
                                        {state.proxy.enableVirtualHosts
                                            ? `https://${state.proxy.virtualHosts?.[0]?.host?.trim() || 'your-domain.example.com'}${
                                                  state.proxy.virtualHosts?.[0]?.path?.trim() || '/'
                                              }`
                                            : `https://gateway.company.com${state.proxy.contextPath?.trim() || '/...'}`}
                                    </div>
                                </div>
                                <div>
                                    <div className="text-xs text-muted-foreground">Upstream URL</div>
                                    <div className="font-medium">{state.proxy.targetUrl || '—'}</div>
                                </div>
                                <div className="sm:col-span-2">
                                    <div className="text-xs text-muted-foreground">Virtual hosts</div>
                                    <div className="font-medium">
                                        {state.proxy.enableVirtualHosts ? `Enabled (${state.proxy.virtualHosts?.length ?? 0})` : 'Disabled'}
                                    </div>
                                </div>
                            </div>
                        ) : null}

                        {section.id === 'secure' ? (
                            <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
                                <div>
                                    <div className="text-xs text-muted-foreground">Authentication</div>
                                    <div className="flex items-center gap-2">
                                        <Badge variant="secondary">{formatSecurityType(state.security.type)}</Badge>
                                    </div>
                                </div>
                                {'planName' in state.security ? (
                                    <div>
                                        <div className="text-xs text-muted-foreground">Plan name</div>
                                        <div className="font-medium">{state.security.planName || '—'}</div>
                                    </div>
                                ) : null}
                                {state.security.type === 'jwt' ? (
                                    <div className="sm:col-span-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                        <div>
                                            <div className="text-xs text-muted-foreground">Signature</div>
                                            <div className="font-medium">{state.security.signature || '—'}</div>
                                        </div>
                                        <div>
                                            <div className="text-xs text-muted-foreground">JWKS resolver</div>
                                            <div className="font-medium">{state.security.jwksResolver || '—'}</div>
                                        </div>
                                        <div className="sm:col-span-2">
                                            <div className="text-xs text-muted-foreground">Resolver parameter</div>
                                            <div className="font-medium">{state.security.resolverParam || '—'}</div>
                                        </div>
                                    </div>
                                ) : null}
                                {state.security.type === 'oauth2' ? (
                                    <div className="sm:col-span-3">
                                        <div className="text-xs text-muted-foreground">Resource</div>
                                        <div className="font-medium">{state.security.resource || '—'}</div>
                                    </div>
                                ) : null}
                            </div>
                        ) : null}
                    </Card>
                );
            })}

            <Card className="rounded-xl border border-success/25 bg-success/5 p-4 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                        <div className="flex items-center gap-2">
                            <CircleCheckIcon className="size-4 text-success" aria-hidden="true" />
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
