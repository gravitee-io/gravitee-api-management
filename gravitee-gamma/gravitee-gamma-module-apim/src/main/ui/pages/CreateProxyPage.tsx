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
import { Badge, Button, Card } from '@gravitee/graphene-core';
import {
    AlertCircleIcon,
    ArrowRightIcon,
    ChevronDownIcon,
    KeyRoundIcon,
    LayoutGridIcon,
    LockIcon,
    PencilIcon,
    ScrollTextIcon,
    ShieldCheckIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { WizardStarter } from './create-proxy/CreateProxyWizard';
import { ApiCreationStoreProvider } from '../domain/apiCreation/useApiCreationStore';
import { apiCreationTemplates } from '../domain/apiCreation/stepRegistry';

const DOCS_URL = 'https://documentation.gravitee.io/apim';

function TemplateIcon({ kind }: Readonly<{ kind: 'api-key' | 'jwt' | 'oauth2' | 'keyless' }>) {
    const Icon = kind === 'api-key' ? KeyRoundIcon : kind === 'jwt' ? ShieldCheckIcon : kind === 'oauth2' ? LockIcon : AlertCircleIcon;
    const iconTone = kind === 'keyless' ? 'text-destructive' : 'text-primary';
    const bgTone = kind === 'keyless' ? 'bg-destructive/10' : 'bg-muted';

    return (
        <div className={`rounded-lg ${bgTone} p-3`}>
            <Icon className={`size-6 ${iconTone}`} aria-hidden="true" />
        </div>
    );
}

function templateFlow(kind: 'api-key' | 'jwt' | 'oauth2' | 'keyless') {
    if (kind === 'jwt') return { nodes: ['Client', 'Gateway', 'Your Backend'], sub: ['Identity Provider'], tags: ['REST', 'JWT', 'JWKS'] as const };
    if (kind === 'oauth2') return { nodes: ['Client', 'Gateway', 'Your Backend'], sub: ['Auth Server'], tags: ['REST', 'OAuth 2.0'] as const };
    if (kind === 'api-key') return { nodes: ['Client', 'Gateway', 'Your Backend'], sub: ['API key check'], tags: ['REST', 'API Key'] as const };
    return { nodes: ['Client', 'Gateway', 'Your Backend'], sub: ['No credentials'], tags: ['REST', 'Keyless'] as const };
}

export function CreateProxyPage() {
    const navigate = useNavigate();
    const [templatesOpen, setTemplatesOpen] = useState(false);
    const [wizardOpen, setWizardOpen] = useState(false);
    const [selectedTemplateId, setSelectedTemplateId] = useState<string | undefined>(undefined);

    const handleExit = useCallback(() => {
        navigate('..');
    }, [navigate]);

    const closeWizard = useCallback(() => {
        setWizardOpen(false);
        setSelectedTemplateId(undefined);
    }, []);

    if (wizardOpen) {
        return (
            <ApiCreationStoreProvider>
                <WizardStarter templateId={selectedTemplateId} onExit={closeWizard} />
            </ApiCreationStoreProvider>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                <div className="min-w-0 space-y-2">
                    <h1 className="text-2xl font-semibold tracking-tight">Create API Proxy</h1>
                    <p className="text-sm text-muted-foreground">Choose the full wizard or a preset, then add your API details.</p>
                </div>
                <Button type="button" variant="outline" size="sm" className="ml-auto h-9 shrink-0 gap-2 self-end sm:self-start" asChild>
                    <a href={DOCS_URL} target="_blank" rel="noopener noreferrer">
                        <ScrollTextIcon className="size-4" aria-hidden="true" />
                        Documentation
                    </a>
                </Button>
            </div>

            <div className="grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2">
                <button
                    type="button"
                    onClick={() => {
                        setSelectedTemplateId(undefined);
                        setWizardOpen(true);
                    }}
                    className="group flex h-full min-h-[11rem] flex-col items-stretch gap-4 rounded-xl border-2 border-primary/30 bg-primary/5 p-6 text-left shadow-sm transition-all hover:border-primary/55 hover:bg-primary/10 hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
                >
                    <div className="flex items-start gap-4">
                        <div className="rounded-lg bg-muted p-3">
                            <PencilIcon className="size-6 text-primary" aria-hidden="true" />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <p className="text-base font-semibold text-foreground">Start from scratch</p>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                                Full four-step wizard: details, entrypoints, security, and review. No preset plans — you choose everything explicitly.
                            </p>
                        </div>
                        <ArrowRightIcon className="size-5 shrink-0 text-primary transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
                    </div>
                </button>

                <button
                    type="button"
                    onClick={() => setTemplatesOpen((o) => !o)}
                    className={`group flex h-full min-h-[11rem] flex-col items-stretch gap-4 rounded-xl border-2 bg-card p-6 text-left shadow-sm transition-all hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                        templatesOpen ? 'border-primary/50 bg-primary/5' : 'border-border hover:border-primary/35'
                    }`}
                >
                    <div className="flex items-start gap-4">
                        <div className="rounded-lg bg-muted p-3">
                            <LayoutGridIcon className="size-6 text-primary" aria-hidden="true" />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <p className="text-base font-semibold text-foreground">Quick-start templates</p>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                                Presets with security and plans filled in. You mainly add name and upstream URL; adjust anything on review.
                            </p>
                        </div>
                        <ChevronDownIcon
                            className={`size-5 shrink-0 text-muted-foreground transition-transform duration-200 ${templatesOpen ? 'rotate-180' : ''}`}
                            aria-hidden="true"
                        />
                    </div>
                </button>
            </div>

            {templatesOpen ? (
                <Card className="rounded-xl p-4 sm:p-6">
                    <div className="space-y-4">
                        <p className="text-sm text-muted-foreground">Pick a template to open essentials with the right auth pattern pre-selected.</p>

                        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                            {apiCreationTemplates.map((t) => {
                                const flow = templateFlow(t.icon);
                                const isNotRecommended = Boolean(t.caution);
                                const isMostCommon = t.headline === 'Most common pattern';

                                return (
                                    <button
                                        key={t.id}
                                        type="button"
                                        onClick={() => {
                                            setSelectedTemplateId(t.id);
                                            setWizardOpen(true);
                                        }}
                                        className={`group flex h-full flex-col items-stretch gap-4 rounded-xl border bg-card p-5 text-left shadow-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                                            isNotRecommended ? 'border-destructive/25' : 'border-border'
                                        }`}
                                    >
                                        <div className="flex items-start gap-4">
                                            <TemplateIcon kind={t.icon} />

                                            <div className="min-w-0 flex-1 space-y-1">
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <p className="text-base font-semibold text-foreground">{t.label}</p>
                                                    {isMostCommon ? <Badge variant="secondary">Most common pattern</Badge> : null}
                                                    {isNotRecommended ? (
                                                        <Badge variant="secondary" className="border border-destructive/30 bg-destructive/10 text-destructive">
                                                            Not recommended
                                                        </Badge>
                                                    ) : null}
                                                </div>
                                                {t.headline ? <p className="text-xs text-muted-foreground">{t.headline}</p> : null}
                                            </div>

                                            <ArrowRightIcon
                                                className="size-5 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                                                aria-hidden="true"
                                            />
                                        </div>

                                        <p className="text-sm text-muted-foreground leading-relaxed">{t.description}</p>

                                        <div className="space-y-2 rounded-lg border bg-muted/20 p-3">
                                            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Request flow</p>
                                            <div className="flex flex-wrap items-center gap-2">
                                                {flow.nodes.map((node, idx) => (
                                                    <div key={node} className="flex items-center gap-2">
                                                        <Badge variant="secondary" className="bg-card">
                                                            {node}
                                                        </Badge>
                                                        {idx < flow.nodes.length - 1 ? <span className="text-xs text-muted-foreground">→</span> : null}
                                                    </div>
                                                ))}
                                            </div>
                                            <div className="flex flex-wrap items-center gap-2">
                                                {flow.sub.map((s) => (
                                                    <Badge key={s} variant="secondary" className="bg-muted">
                                                        {s}
                                                    </Badge>
                                                ))}
                                            </div>
                                        </div>

                                        {t.caution ? (
                                            <div className="rounded-lg border border-destructive/25 bg-destructive/5 p-3">
                                                <div className="flex items-start gap-2">
                                                    <AlertCircleIcon className="mt-0.5 size-4 text-destructive" aria-hidden="true" />
                                                    <div className="space-y-1">
                                                        <p className="text-sm font-medium text-foreground">{t.caution.label}</p>
                                                        <p className="text-xs text-muted-foreground leading-relaxed">{t.caution.description}</p>
                                                    </div>
                                                </div>
                                            </div>
                                        ) : null}

                                        <div className="mt-auto flex flex-wrap items-center justify-between gap-2 pt-1">
                                            <div className="flex flex-wrap items-center gap-2">
                                                {t.tags.map((tag) => (
                                                    <Badge key={tag} variant="secondary" className="bg-muted/40">
                                                        {tag}
                                                    </Badge>
                                                ))}
                                            </div>
                                            <Badge variant="secondary">{t.steps.length} steps</Badge>
                                        </div>
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </Card>
            ) : null}

            <div className="flex items-center justify-between border-t pt-4">
                <Button type="button" variant="outline" size="sm" onClick={handleExit}>
                    Exit
                </Button>
                <span className="text-xs text-muted-foreground max-w-md text-right">
                    Pick Start from scratch or open Quick-start templates, then select a preset.
                </span>
            </div>
        </div>
    );
}

