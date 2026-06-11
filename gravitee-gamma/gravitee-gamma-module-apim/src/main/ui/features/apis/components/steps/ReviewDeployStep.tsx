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
import { Badge, Button, Switch } from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronUpIcon, GlobeIcon, PencilIcon, RocketIcon, ServerIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';
import { useState } from 'react';

import { SecurityPlanFields } from './SecurityPlanFields';
import { useGatewayPrefix } from '../../hooks/useGatewayPrefix';
import { useApiCreation } from '../../store/apiCreationStore';
import { buildPlanName, buildPreviewGatewayUrl } from '../../utils/apiProxyMapper';
import { AUTH_LABEL } from '../../utils/securityFormatters';
import { ReviewRow } from '../ReviewRow';

interface SectionHeaderProps {
    title: string;
    icon?: ReactNode;
    onEdit?: () => void;
    trailing?: ReactNode;
}

function SectionHeader({ title, icon, onEdit, trailing }: SectionHeaderProps) {
    return (
        <div className="flex items-center justify-between border-b px-4 py-3 bg-muted/30">
            <div className="flex items-center gap-2">
                {icon}
                <p className="text-xs font-semibold text-muted-foreground" style={{ letterSpacing: '0.06em', textTransform: 'uppercase' }}>
                    {title}
                </p>
            </div>
            {onEdit && (
                <Button variant="ghost" size="sm" onClick={onEdit} className="h-7 px-2 text-xs gap-1">
                    <PencilIcon className="size-3" aria-hidden />
                    Edit
                </Button>
            )}
            {trailing}
        </div>
    );
}

export function ReviewDeployStep() {
    const { state, dispatch } = useApiCreation();
    const { form, creationMode } = state;
    const isTemplate = creationMode === 'template';

    const [customizeSecurity, setCustomizeSecurity] = useState(false);

    function update(patch: Partial<typeof form>) {
        dispatch({ type: 'UPDATE_FORM', patch });
    }

    function goToStep(step: number) {
        dispatch({ type: 'CLEAR_VALIDATION_ERRORS' });
        dispatch({ type: 'SET_STEP', step });
    }

    const gatewayPrefix = useGatewayPrefix();
    const gatewayUrl = buildPreviewGatewayUrl(form, gatewayPrefix);
    const planName = buildPlanName(form);

    // template: step 0 = Essentials (covers details + proxy + security)
    // scratch: step 0 = Details, step 1 = Entrypoints, step 2 = Security
    const detailsStep = 0;
    const proxyStep = isTemplate ? 0 : 1;
    const securityStep = isTemplate ? null : 2; // template uses inline Customize instead

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">Review & Deploy</h2>
                <p className="text-sm text-muted-foreground">Check the details below before creating your API proxy.</p>
            </div>

            <div className="space-y-4">
                {/* API Details */}
                <div className="rounded-xl border bg-card overflow-hidden">
                    <SectionHeader title="API Details" onEdit={() => goToStep(detailsStep)} />
                    <ReviewRow label="Name" value={form.apiName} />
                    <ReviewRow label="Version" value={form.apiVersion} />
                    {form.apiDescription && <ReviewRow label="Description" value={form.apiDescription} />}
                    <ReviewRow label="Protocol">
                        <Badge variant="secondary" className="text-xs">
                            REST
                        </Badge>
                    </ReviewRow>
                </div>

                {/* Proxy Configuration */}
                <div className="rounded-xl border bg-card overflow-hidden">
                    <SectionHeader title="Proxy Configuration" onEdit={() => goToStep(proxyStep)} />
                    <div className="flex items-start justify-between gap-4 px-4 py-2.5 border-b">
                        <span className="text-xs text-muted-foreground shrink-0 flex items-center gap-1.5">
                            <GlobeIcon className="size-3" aria-hidden />
                            Gateway path
                        </span>
                        <span
                            className="text-xs font-medium font-mono text-right"
                            style={{ maxWidth: '60%', wordBreak: 'break-word', overflowWrap: 'break-word' }}
                        >
                            {gatewayUrl}
                        </span>
                    </div>
                    <div className="flex items-start justify-between gap-4 px-4 py-2.5">
                        <span className="text-xs text-muted-foreground shrink-0 flex items-center gap-1.5">
                            <ServerIcon className="size-3" aria-hidden />
                            Upstream
                        </span>
                        <span
                            className="text-xs font-medium font-mono text-right"
                            style={{ maxWidth: '60%', wordBreak: 'break-word', overflowWrap: 'break-word' }}
                        >
                            {form.targetUrl || <span className="text-muted-foreground font-sans italic">Not set</span>}
                        </span>
                    </div>
                </div>

                {/* Security */}
                <div className="rounded-xl border bg-card overflow-hidden">
                    <SectionHeader
                        title="Security"
                        icon={<ShieldIcon className="size-3.5 text-primary" aria-hidden />}
                        onEdit={securityStep !== null ? () => goToStep(securityStep) : undefined}
                        trailing={
                            isTemplate ? (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => setCustomizeSecurity(v => !v)}
                                    className="h-7 px-2 text-xs gap-1"
                                >
                                    {customizeSecurity ? (
                                        <ChevronUpIcon className="size-3" aria-hidden />
                                    ) : (
                                        <ChevronDownIcon className="size-3" aria-hidden />
                                    )}
                                    Customize
                                </Button>
                            ) : undefined
                        }
                    />

                    <div className="flex items-center justify-between gap-4 px-4 py-2.5 border-b">
                        <span className="text-xs text-muted-foreground">Auth type</span>
                        <Badge variant="secondary">{AUTH_LABEL[form.authType]}</Badge>
                    </div>
                    <ReviewRow label="Plan name" value={planName} />

                    {isTemplate && customizeSecurity && (
                        <div className="px-4 pb-4 pt-3 border-t">
                            <SecurityPlanFields showAuthSelector={true} />
                        </div>
                    )}
                </div>

                {/* Deploy toggle */}
                <div className="flex items-center gap-4 rounded-xl border p-4">
                    <div className="rounded-lg bg-primary/10 p-2 shrink-0">
                        <RocketIcon className="size-5 text-primary" aria-hidden />
                    </div>
                    <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold">Deploy and start API immediately</p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                            The API will be live on the gateway as soon as it is created. Disable to save as a draft first.
                        </p>
                    </div>
                    <Switch
                        checked={form.deployImmediately}
                        onCheckedChange={v => update({ deployImmediately: v })}
                        aria-label="Deploy immediately"
                    />
                </div>
            </div>
        </div>
    );
}
