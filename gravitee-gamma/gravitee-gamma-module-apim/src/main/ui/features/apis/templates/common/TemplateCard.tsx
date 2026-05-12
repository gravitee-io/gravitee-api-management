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
import { Badge } from '@gravitee/graphene-core';
import { AlertCircleIcon, ArrowRightIcon, KeyRoundIcon, LockIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import type { ApiCreationTemplate, TemplateIconKind } from '../../types/template.types';

type FlowMeta = { sub: string; note?: string };

function getFlowMeta(icon: TemplateIconKind): FlowMeta {
    switch (icon) {
        case 'api-key':
            return { sub: 'API Key check' };
        case 'jwt':
            return { sub: 'Identity Provider', note: 'JWT validated via JWKS endpoint' };
        case 'oauth2':
            return { sub: 'Auth Server', note: 'OAuth 2.0 token introspection' };
        case 'keyless':
            return { sub: 'No credentials' };
    }
}

function TemplateIcon({ kind }: Readonly<{ kind: TemplateIconKind }>) {
    const destructive = kind === 'keyless';
    const Icon = kind === 'api-key' ? KeyRoundIcon : kind === 'jwt' ? ShieldCheckIcon : kind === 'oauth2' ? LockIcon : AlertCircleIcon;
    return (
        <div className={`shrink-0 rounded-lg p-3 ${destructive ? 'bg-destructive/10' : 'bg-muted'}`}>
            <Icon className={`size-6 ${destructive ? 'text-destructive' : 'text-primary'}`} aria-hidden="true" />
        </div>
    );
}

function RequestFlowDiagram({ icon }: Readonly<{ icon: TemplateIconKind }>) {
    const { sub, note } = getFlowMeta(icon);

    return (
        <div className="space-y-1.5 rounded-lg border border-border bg-muted/40 p-3">
            <p className="font-medium uppercase tracking-wide text-muted-foreground" style={{ fontSize: '10px' }}>
                Request flow
            </p>

            <div className="grid grid-cols-5 gap-x-1 gap-y-1">
                {/* ── Row 1: flow nodes ── */}
                <div className="col-start-1 row-start-1 flex items-center justify-center">
                    <Badge variant="secondary" className="bg-muted whitespace-nowrap" style={{ fontSize: '10px' }}>
                        Client
                    </Badge>
                </div>

                <div className="col-start-2 row-start-1 flex items-center justify-center">
                    <ArrowRightIcon className="size-3 shrink-0 text-muted-foreground" aria-hidden="true" />
                </div>

                <div className="col-start-3 row-start-1 flex items-center justify-center">
                    <Badge
                        variant="secondary"
                        className="border border-primary/20 bg-primary/10 text-primary whitespace-nowrap"
                        style={{ fontSize: '10px' }}
                    >
                        Gateway
                    </Badge>
                </div>

                <div className="col-start-4 row-start-1 flex items-center justify-center">
                    <ArrowRightIcon className="size-3 shrink-0 text-muted-foreground" aria-hidden="true" />
                </div>

                <div className="col-start-5 row-start-1 flex items-center justify-center">
                    <Badge variant="secondary" className="bg-muted whitespace-nowrap" style={{ fontSize: '10px' }}>
                        Backend
                    </Badge>
                </div>

                {/* ── Row 2: spans cols 2–4 so text centers under Gateway without clipping ── */}
                <div className="col-start-2 col-span-3 row-start-2 flex justify-center">
                    <span className="text-center text-xs font-medium leading-tight text-muted-foreground">{sub}</span>
                </div>
            </div>

            {note ? (
                <p className="text-center text-muted-foreground" style={{ fontSize: '10px' }}>
                    {note}
                </p>
            ) : null}
        </div>
    );
}

type TemplateCardProps = Readonly<{
    template: ApiCreationTemplate;
    onClick: () => void;
}>;

export function TemplateCard({ template, onClick }: TemplateCardProps) {
    const isFeatured = Boolean(template.featured);
    const isNotRecommended = Boolean(template.caution);
    const inputScreens = template.steps.filter(s => s !== 'review-deploy').length;
    const setupLabel = inputScreens <= 2 ? 'Quick setup' : 'Guided setup';

    return (
        <button
            type="button"
            onClick={onClick}
            className={`group flex h-full flex-col items-stretch gap-4 rounded-xl border bg-card p-5 text-left shadow-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                isNotRecommended ? 'border-destructive/25' : 'border-border'
            }`}
        >
            {/* Header */}
            <div className="flex items-start gap-4">
                <TemplateIcon kind={template.icon} />
                <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                        <p className="text-base font-semibold text-foreground">{template.label}</p>
                        {isFeatured ? <Badge variant="secondary">Most common pattern</Badge> : null}
                        {isNotRecommended ? (
                            <Badge variant="secondary" className="border border-destructive/30 bg-destructive/10 text-destructive">
                                Not recommended
                            </Badge>
                        ) : null}
                    </div>
                    {template.headline ? <p className="text-xs text-muted-foreground">{template.headline}</p> : null}
                </div>
                <ArrowRightIcon
                    className="size-5 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                    aria-hidden="true"
                />
            </div>

            {/* Description */}
            <p className="text-sm leading-relaxed text-muted-foreground">{template.description}</p>

            {/* Flow diagram */}
            <RequestFlowDiagram icon={template.icon} />

            {/* Caution box */}
            {template.caution ? (
                <div className="rounded-lg border border-destructive/25 bg-destructive/5 p-3">
                    <div className="flex items-start gap-2">
                        <AlertCircleIcon className="mt-0.5 size-4 shrink-0 text-destructive" aria-hidden="true" />
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-foreground">{template.caution.label}</p>
                            <p className="text-xs leading-relaxed text-muted-foreground">{template.caution.description}</p>
                        </div>
                    </div>
                </div>
            ) : null}

            {/* Footer */}
            <div className="mt-auto flex flex-wrap items-center justify-between gap-2 pt-1">
                <div className="flex flex-wrap items-center gap-2">
                    {template.tags.map(tag => (
                        <Badge key={tag} variant="secondary" className="bg-muted/40">
                            {tag}
                        </Badge>
                    ))}
                </div>
                <Badge variant="secondary">{setupLabel}</Badge>
            </div>
        </button>
    );
}
