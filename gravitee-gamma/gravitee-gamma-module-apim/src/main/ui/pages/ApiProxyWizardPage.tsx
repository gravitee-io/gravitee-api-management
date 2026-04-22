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
import { Alert, AlertDescription, AlertTitle, Button, Card, CardContent, cn } from '@gravitee/graphene-core';
import * as Collapsible from '@radix-ui/react-collapsible';
import { AlertTriangle, ArrowRight, BookOpen, ChevronDown, ExternalLink, Info, KeyRound, Lock, Pencil, ShieldCheck, Unlock } from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

import { resolveModulePath } from '../config/routes';

const DOCS_BASE = 'https://documentation.gravitee.io/apim';

type TemplateTone = 'blue' | 'violet' | 'amber' | 'rose';

type ProxyTemplate = {
    readonly id: string;
    readonly title: string;
    readonly subtitle: string;
    readonly description: string;
    readonly tone: TemplateTone;
    readonly tags: readonly string[];
    readonly notRecommended?: boolean;
    readonly warningMessage?: string;
    readonly icon: React.ComponentType<{ className?: string }>;
};

function toneClassName(tone: TemplateTone): { readonly icon: string; readonly surface: string } {
    switch (tone) {
        case 'blue':
            return { icon: 'text-blue-500', surface: 'bg-blue-500/10' };
        case 'violet':
            return { icon: 'text-violet-500', surface: 'bg-violet-500/10' };
        case 'amber':
            return { icon: 'text-amber-500', surface: 'bg-amber-500/10' };
        case 'rose':
            return { icon: 'text-rose-600 dark:text-rose-400', surface: 'bg-rose-500/10' };
    }
}

function ProxyTemplateCard(props: { readonly template: ProxyTemplate; readonly onSelect: (id: string) => void }) {
    const { template, onSelect } = props;
    const Icon = template.icon;
    const tone = toneClassName(template.tone);

    return (
        <button
            type="button"
            onClick={() => onSelect(template.id)}
            className={cn(
                'flex flex-col gap-3 rounded-xl border bg-card p-5 text-left transition-all',
                'hover:border-primary/50 hover:bg-primary/5',
                template.notRecommended ? 'border-amber-500/25 hover:border-amber-500/40' : 'border-muted',
            )}
        >
            <div className="flex items-start gap-3">
                <div className={cn('rounded-lg p-2.5 shrink-0', tone.surface)}>
                    <Icon className={cn('size-5', tone.icon)} />
                </div>
                <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                        <div className="text-sm font-medium leading-snug">{template.title}</div>
                        {template.notRecommended ? (
                            <span className="rounded-full border border-amber-500/50 bg-amber-500/10 px-2 py-0.5 text-[10px] text-amber-800 dark:text-amber-200">
                                Not recommended
                            </span>
                        ) : null}
                    </div>
                    <div className="text-xs text-muted-foreground">{template.subtitle}</div>
                </div>
            </div>

            <div className="text-xs leading-relaxed text-muted-foreground">{template.description}</div>

            {template.notRecommended && template.warningMessage ? (
                <Alert className="border-amber-500/40 bg-amber-500/5 py-2">
                    <AlertTriangle className="size-4 text-amber-600 dark:text-amber-400" />
                    <AlertTitle className="text-xs font-semibold text-amber-900 dark:text-amber-100">Demo and testing only</AlertTitle>
                    <AlertDescription className="text-[11px] leading-snug text-amber-950/80 dark:text-amber-50/90">
                        {template.warningMessage}
                    </AlertDescription>
                </Alert>
            ) : null}

            <div className="flex flex-wrap gap-1.5">
                {template.tags.map(tag => (
                    <span key={tag} className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
                        {tag}
                    </span>
                ))}
            </div>
        </button>
    );
}

function LearnMoreHttpProxy() {
    const [learnMoreOpen, setLearnMoreOpen] = useState(false);

    return (
        <Collapsible.Root open={learnMoreOpen} onOpenChange={setLearnMoreOpen}>
            <div className="rounded-xl border bg-card overflow-hidden">
                <Collapsible.Trigger asChild>
                    <button
                        type="button"
                        className={cn(
                            'flex w-full items-start justify-between gap-4 p-6 text-left rounded-xl',
                            'min-h-[104px] transition-colors hover:bg-muted/50',
                            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                        )}
                    >
                        <div className="min-w-0">
                            <p className="text-base font-semibold text-foreground">Learn more: HTTP API proxies</p>
                            <p className="text-sm text-muted-foreground mt-1 leading-relaxed">
                                You’re configuring an HTTP proxy: a managed front door in front of an existing REST (or similar) upstream service.
                            </p>
                        </div>
                        <ChevronDown
                            className={cn(
                                'size-5 shrink-0 text-muted-foreground transition-transform duration-200 mt-1',
                                learnMoreOpen ? 'rotate-180' : '',
                            )}
                            aria-hidden
                        />
                    </button>
                </Collapsible.Trigger>
                <Collapsible.Content>
                    <div className="space-y-4 border-t bg-muted/20 px-6 py-5">
                        <div className="rounded-lg border bg-card/80 p-4">
                            <p className="mb-3 text-center text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                                How an HTTP API proxy fits together
                            </p>
                            <div className="flex flex-wrap items-center justify-center gap-2">
                                <span className="rounded-md bg-muted border px-2.5 py-1.5 text-xs font-mono">Client</span>
                                <ArrowRight className="size-3 text-primary shrink-0" aria-hidden />
                                <span className="rounded-md bg-primary/10 border border-primary/20 px-2.5 py-1.5 text-xs font-mono text-primary">
                                    Gateway
                                </span>
                                <ArrowRight className="size-3 text-primary shrink-0" aria-hidden />
                                <span className="rounded-md bg-muted border px-2.5 py-1.5 text-xs font-mono">Upstream API</span>
                            </div>
                            <p className="mx-auto mt-3 max-w-lg text-center text-xs text-muted-foreground leading-relaxed">
                                Apps call your published gateway URL. The gateway applies entrypoints, plans, and policies, then proxies the HTTP request to your
                                backend service.
                            </p>
                        </div>

                        <div className="flex flex-wrap gap-2">
                            <a
                                href={`${DOCS_BASE}/using-apim/api-creation`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-1.5 rounded-md border border-border/40 bg-card/50 px-3 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary/5 hover:border-primary/30"
                            >
                                API creation & HTTP proxy
                                <ExternalLink className="size-3" aria-hidden />
                            </a>
                            <a
                                href={`${DOCS_BASE}/getting-started/configuration/v4-api-configuration/entrypoints`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-1.5 rounded-md border border-border/40 bg-card/50 px-3 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary/5 hover:border-primary/30"
                            >
                                Entrypoints & listeners
                                <ExternalLink className="size-3" aria-hidden />
                            </a>
                            <a
                                href={`${DOCS_BASE}/using-apim/api-policies`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-1.5 rounded-md border border-border/40 bg-card/50 px-3 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary/5 hover:border-primary/30"
                            >
                                Plans, policies, and security
                                <ExternalLink className="size-3" aria-hidden />
                            </a>
                        </div>
                    </div>
                </Collapsible.Content>
            </div>
        </Collapsible.Root>
    );
}

export function ApiProxyWizardPage() {
    const [templatesOpen, setTemplatesOpen] = useState(false);
    const location = useLocation();

    const { modulePrefix } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);
    const exitTo = useMemo(() => (modulePrefix ? `/${modulePrefix}/apis` : '/apis'), [modulePrefix]);

    const templates = useMemo<readonly ProxyTemplate[]>(
        () => [
            {
                id: 'rest-api-key',
                title: 'REST API with API Key',
                subtitle: 'Most common pattern',
                description: 'Protect your REST API with simple API key authentication. Consumers receive a key when they subscribe to the plan.',
                tone: 'blue',
                icon: KeyRound,
                tags: ['REST', 'API Key', 'Simple onboarding'],
            },
            {
                id: 'rest-jwt',
                title: 'REST API with JWT',
                subtitle: 'Enterprise identity provider',
                description: 'Validate JWTs issued by your identity provider. Best for organisations with an existing IdP.',
                tone: 'violet',
                icon: ShieldCheck,
                tags: ['REST', 'JWT', 'JWKS', 'Enterprise'],
            },
            {
                id: 'rest-oauth2',
                title: 'REST API with OAuth 2.0',
                subtitle: 'Token-based enterprise security',
                description: 'Enforce OAuth 2.0 access tokens. Ideal for enterprise APIs that require delegated authorization.',
                tone: 'amber',
                icon: Lock,
                tags: ['REST', 'OAuth 2.0', 'Enterprise'],
            },
            {
                id: 'rest-keyless',
                title: 'REST API with Keyless plan',
                subtitle: 'Not recommended',
                description: 'Creates a REST proxy with a keyless (open) plan so traffic is accepted without API keys or subscriptions.',
                tone: 'rose',
                icon: Unlock,
                tags: ['REST', 'Keyless', 'Demo / sandbox'],
                notRecommended: true,
                warningMessage:
                    'For demos, workshops, and local testing only. The API is publicly reachable without subscriptions or API keys. Do not use for production.',
            },
        ],
        [],
    );

    const handleSelectTemplate = useCallback((id: string) => {
        // TODO (next iteration): hook into actual wizard flow (template mode / steps).
        // For now we keep the template picker experience and provide a stable place to wire future steps.
        // eslint-disable-next-line no-console
        console.log('Selected template', id);
    }, []);

    return (
        <div className={cn('flex flex-col gap-6 p-6')}>
            <header>
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <h1 className="font-semibold text-xl">Create API Proxy</h1>
                        <p className="text-muted-foreground">
                            Start from scratch for the full wizard, or expand quick-start templates when you want a preset.
                        </p>
                    </div>

                    <a
                        href={DOCS_BASE}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 self-start text-xs text-primary hover:text-primary/80 hover:underline underline-offset-2"
                    >
                        <BookOpen className="size-3.5" aria-hidden />
                        Gravitee API Management documentation
                        <ExternalLink className="size-3" aria-hidden />
                    </a>
                </div>
            </header>

            <Alert className="rounded-xl border p-5 bg-muted/30">
            <Info className="size-4 shrink-0 text-blue-500 mt-0.5" aria-hidden />
                <AlertDescription className="text-muted-foreground text-sm">
                    Templates pre-fill security and configuration so you only need to provide your API name and upstream URL. You can always customize
                    everything before deploying.
                </AlertDescription>
            </Alert>

            <Card className="rounded-xl border-2 border-primary/30 bg-primary/5 shadow-sm">
                <CardContent className="p-0">
                    <button
                        type="button"
                        className={cn(
                            'w-full text-left flex items-start gap-4 p-6 rounded-xl',
                            'transition-all hover:border-primary/55 hover:bg-primary/10',
                            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40',
                        )}
                        aria-label="Start from scratch"
                        onClick={() => {}}
                    >
                        <div className="rounded-lg bg-primary/15 p-3">
                            <Pencil className="size-6 text-primary" aria-hidden />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <div className="text-base font-semibold text-foreground">Start from scratch</div>
                            <div className="text-sm text-muted-foreground leading-relaxed">
                                Open the full four-step wizard with no preset security or plans. Best default when you want explicit control over entrypoints,
                                authentication, and review before deploy.
                            </div>
                        </div>
                        <ArrowRight className="size-5 shrink-0 text-primary mt-1" aria-hidden />
                    </button>
                </CardContent>
            </Card>

            <Collapsible.Root open={templatesOpen} onOpenChange={setTemplatesOpen}>
                <div className="rounded-xl border bg-card overflow-hidden">
                    <Collapsible.Trigger asChild>
                        <button
                            type="button"
                            className={cn(
                                'flex w-full items-start justify-between gap-4 p-6 text-left rounded-xl',
                                'min-h-[104px] transition-colors hover:bg-muted/50',
                                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                            )}
                        >
                            <div className="min-w-0">
                                <p className="text-base font-semibold text-foreground">Quick-start templates</p>
                                <p className="text-sm text-muted-foreground mt-1 leading-relaxed">
                                    Optional presets — fewer fields, pre-filled plans and auth. Expand to compare options.
                                </p>
                            </div>
                            <ChevronDown
                                className={cn(
                                    'size-5 shrink-0 text-muted-foreground transition-transform duration-200 mt-1',
                                    templatesOpen ? 'rotate-180' : '',
                                )}
                                aria-hidden
                            />
                        </button>
                    </Collapsible.Trigger>
                    <Collapsible.Content>
                        <div className="space-y-4 border-t bg-muted/20 px-4 py-4">
                            <div className="flex items-start gap-3 rounded-lg border border-blue-500/20 bg-blue-500/5 px-3 py-2.5">
                                <Info className="size-4 shrink-0 text-blue-500 mt-0.5" aria-hidden />
                                <p className="text-xs text-muted-foreground leading-relaxed">
                                    Templates pre-fill security and plans so you mainly add API name and upstream URL. You can still change anything on the review
                                    step before deploying.
                                </p>
                            </div>

                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                {templates.map(template => (
                                    <ProxyTemplateCard key={template.id} template={template} onSelect={handleSelectTemplate} />
                                ))}
                            </div>
                        </div>
                    </Collapsible.Content>
                </div>
            </Collapsible.Root>

            <LearnMoreHttpProxy />

            <div className="pt-2">
                <Button asChild type="button" variant="ghost">
                    <Link to={exitTo}>Exit</Link>
                </Button>
            </div>
        </div>
    );
}

