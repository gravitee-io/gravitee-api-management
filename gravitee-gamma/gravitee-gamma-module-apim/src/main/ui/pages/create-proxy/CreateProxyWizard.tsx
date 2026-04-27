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
import { Badge, Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input, Label, Switch } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CircleCheckIcon,
    FileTextIcon,
    GlobeIcon,
    InfoIcon,
    PlusIcon,
    ScrollTextIcon,
    ServerIcon,
    ShieldIcon,
    Trash2Icon,
    XIcon,
} from '@gravitee/graphene-core/icons';
import { Globe, Sparkles, Zap } from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, type RefObject } from 'react';
import { useNavigate } from 'react-router-dom';

import { FormRenderer } from '../../components/form/FormRenderer';
import { ReviewRenderer } from '../../components/form/ReviewRenderer';
import { proxyContextPathInputPattern, proxyContextPathInputTitle } from '../../domain/apiCreation/fieldRegistry';
import { useApiCreationStore } from '../../domain/apiCreation/useApiCreationStore';
import { useDebouncedVerifyPaths } from './apiCreation/queries/useDebouncedVerifyPaths';
import type { CreateProxyWorkflowResult } from './apiCreation/createProxyWorkflow';
import { getApimErrorMessage, useCreateProxyMutation } from './apiCreation/queries/useCreateProxyMutation';
import { useProxyConnectorBootstrap } from './apiCreation/queries/useProxyConnectorBootstrap';

const DOCS_URL = 'https://documentation.gravitee.io/apim';

const labelForSecurityType = (type: ReturnType<typeof useApiCreationStore>['state']['data']['security']['type']): string => {
    switch (type) {
        case 'keyless':
            return 'Keyless (Open)';
        case 'api-key':
            return 'API Key';
        case 'jwt':
            return 'JWT';
        case 'oauth2':
            return 'OAuth 2.0';
        case 'mtls':
            return 'mTLS';
    }
};

function footerActionLabel(stepId: string | undefined, stepLabel: string | undefined, deployImmediately: boolean) {
    if (!stepId || !stepLabel) return '';
    if (stepId === 'review-deploy') return deployImmediately ? 'Deploy Proxy' : 'Create Proxy';
    if (stepId === 'api-details') return 'Validate my API details';
    if (stepId === 'configure-proxy') return 'Validate my entrypoints';
    if (stepId === 'secure') return 'Validate my security';
    return 'Validate my ' + stepLabel.toLowerCase();
}

function RequestPathCard({
    stepId,
    contextPath,
    enableVirtualHosts,
    virtualHosts,
    targetUrl,
    securityType,
}: Readonly<{
    stepId: string | undefined;
    contextPath: string;
    enableVirtualHosts: boolean;
    virtualHosts: ReadonlyArray<{ host: string; path: string; overrideAccess: boolean }>;
    targetUrl: string;
    securityType: ReturnType<typeof useApiCreationStore>['state']['data']['security']['type'];
}>) {
    const gatewayUrl = useMemo(() => {
        if (enableVirtualHosts) {
            const first = virtualHosts?.[0];
            const host = first?.host?.trim() || 'your-domain.example.com';
            const path = first?.path?.trim() || '/';
            return `https://${host}${path}`;
        }

        const path = contextPath?.trim() || '/...';
        return `https://gateway.company.com${path}`;
    }, [contextPath, enableVirtualHosts, virtualHosts]);

    const upstreamUrl = useMemo(() => targetUrl?.trim() || 'https://upstream.example.com', [targetUrl]);

    const footerHint = useMemo(() => {
        if (stepId === 'api-details') return 'Defining API identity';
        if (stepId === 'configure-proxy') return 'Configuring entrypoints & upstream';
        if (stepId === 'secure') return 'Configuring security & access';
        return 'Ready to review & deploy';
    }, [stepId]);

    return (
        <Card className="rounded-xl p-4 sm:p-5">
            <div className="text-center text-sm font-semibold">Request path</div>

            <div className="mt-4 space-y-2">
                <div className="rounded-lg border border-dashed bg-muted/40 px-3 py-1 text-center text-xs font-medium text-foreground">
                    <span className="font-mono break-all">Client</span>
                </div>

                <div className="flex justify-center">
                    <ArrowRightIcon className="size-4 text-muted-foreground" style={{ transform: 'rotate(90deg)' }} aria-hidden="true" />
                </div>

                <div className="rounded-lg border border-dashed bg-muted/40 px-3 py-1 text-center text-xs font-medium text-foreground" title={gatewayUrl}>
                    <div className="font-mono break-all">{gatewayUrl}</div>
                </div>

                <div className="flex justify-center">
                    <ArrowRightIcon className="size-4 text-muted-foreground" style={{ transform: 'rotate(90deg)' }} aria-hidden="true" />
                </div>

                <div className="rounded-lg border border-dashed bg-muted/40 px-3 py-1 text-center text-xs font-medium text-foreground">
                    <Badge variant="secondary" className="font-mono break-all">{labelForSecurityType(securityType)}</Badge>
                </div>

                <div className="flex justify-center">
                    <ArrowRightIcon className="size-4 text-muted-foreground" style={{ transform: 'rotate(90deg)' }} aria-hidden="true" />
                </div>

                <div className="rounded-lg border border-dashed bg-muted/40 px-3 py-1 text-center text-xs font-medium text-foreground" title={upstreamUrl}>
                    <span className="font-mono break-all">{upstreamUrl}</span>
                </div>

                <div className="bg-muted/20 px-3 py-2.5 text-xs text-muted-foreground">
                    <div className="text-center">{footerHint}</div>
                </div>
            </div>
        </Card>
    );
}

type VirtualHostRow = Readonly<{ host: string; path: string; overrideAccess: boolean }>;

function ConfigureProxyStepContent({
    contextPath,
    enableVirtualHosts,
    targetUrl,
    contextPathInputRef,
    configureProxyRootRef,
    targetUrlInputRef,
    virtualHosts,
    errors,
    serverPathError,
    pathVerifyPending,
    onChangeContextPath,
    onChangeEnableVirtualHosts,
    onChangeTargetUrl,
    onChangeVirtualHosts,
}: Readonly<{
    contextPath: string;
    enableVirtualHosts: boolean;
    targetUrl: string;
    contextPathInputRef: RefObject<HTMLInputElement | null>;
    configureProxyRootRef: RefObject<HTMLDivElement | null>;
    targetUrlInputRef: RefObject<HTMLInputElement | null>;
    virtualHosts: readonly VirtualHostRow[];
    errors: Record<string, string>;
    serverPathError?: string;
    pathVerifyPending?: boolean;
    onChangeContextPath: (next: string) => void;
    onChangeEnableVirtualHosts: (next: boolean) => void;
    onChangeTargetUrl: (next: string) => void;
    onChangeVirtualHosts: (next: readonly VirtualHostRow[]) => void;
}>) {
    const toggleLabel = enableVirtualHosts ? 'Disable virtual hosts' : 'Enable virtual hosts';
    const ToggleIcon = enableVirtualHosts ? XIcon : CircleCheckIcon;

    const virtualHostsError = errors['proxy.virtualHosts'];
    const contextPathError = errors['proxy.contextPath'];
    const targetUrlError = errors['proxy.targetUrl'];

    return (
        <div ref={configureProxyRootRef} className="space-y-6">
            <Card className="rounded-xl border">
                <div className="px-4 pt-6 pb-3">
                    <div className="flex items-center gap-2 text-base font-semibold">
                        <GlobeIcon className="size-4 text-primary" aria-hidden="true" />
                        Configure common entrypoints fields
                    </div>
                    <div className="mt-1 text-sm text-muted-foreground">How consumers will access this API through the gateway.</div>
                </div>

                <div className="px-4 pb-6">
                    <div className="rounded-xl border">
                        <div className="flex items-center justify-between border-b bg-muted/30 px-4 py-3">
                            <span className="text-sm font-semibold">Entrypoints context</span>
                            <button
                                type="button"
                                onClick={() => onChangeEnableVirtualHosts(!enableVirtualHosts)}
                                className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground"
                            >
                                <ToggleIcon className="size-3.5" aria-hidden="true" />
                                {toggleLabel}
                            </button>
                        </div>

                        {!enableVirtualHosts ? (
                            <div className="space-y-2 p-4">
                                <Label htmlFor="context-path">
                                    Context Path <span className="text-destructive">*</span>
                                </Label>
                                <div className="flex items-center gap-2">
                                    <span className="whitespace-nowrap font-mono text-sm text-muted-foreground">https://gateway.company.com</span>
                                    <Input
                                        ref={contextPathInputRef}
                                        id="context-path"
                                        className="font-mono"
                                        placeholder="/your-api"
                                        pattern={proxyContextPathInputPattern}
                                        title={proxyContextPathInputTitle}
                                        aria-invalid={Boolean(contextPathError)}
                                        value={contextPath}
                                        onChange={(e) => onChangeContextPath(e.target.value)}
                                    />
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    Must start with a <code className="text-[11px]">/</code>. Default follows your API name; edit as needed.
                                </p>
                                {contextPathError ? <p className="text-xs text-destructive">{contextPathError}</p> : null}
                                {pathVerifyPending ? (
                                    <p className="text-xs text-muted-foreground">Checking path availability…</p>
                                ) : null}
                                {serverPathError && !enableVirtualHosts ? (
                                    <p className="text-xs text-destructive">{serverPathError}</p>
                                ) : null}
                            </div>
                        ) : (
                            <div className="space-y-3 p-4">
                                <div className="overflow-hidden rounded-lg border">
                            <div className="grid grid-cols-3 gap-x-3 bg-muted/40 px-3 py-2.5 text-xs">
                                <div>
                                    <p className="font-semibold text-foreground">Virtual Host</p>
                                    <p className="text-muted-foreground">Host that must be set in the HTTP request to access your entrypoint.</p>
                                </div>
                                <div>
                                    <p className="inline-flex items-center gap-1 font-semibold text-foreground">
                                        Context-path <InfoIcon className="size-3 text-muted-foreground" aria-hidden="true" />
                                    </p>
                                    <p className="text-muted-foreground">
                                        Must start with a <code className="text-[10px]">/</code> and can contain an uppercase letter, number, dash or underscore.
                                    </p>
                                </div>
                                <div>
                                    <p className="font-semibold text-foreground">Override access</p>
                                    <p className="text-muted-foreground">Enable override on the access URL of your Portal using virtual host.</p>
                                </div>
                            </div>

                            <div className="divide-y">
                                {(virtualHosts.length ? virtualHosts : [{ host: '', path: '/', overrideAccess: false }]).map((row, idx) => (
                                    <div key={idx} className="grid grid-cols-3 items-center gap-x-3 px-3 py-3">
                                        <Input
                                            placeholder="Host *"
                                            aria-invalid={Boolean(virtualHostsError)}
                                            value={row.host}
                                            onChange={(e) => {
                                                const next = [...virtualHosts];
                                                next[idx] = { ...next[idx], host: e.target.value };
                                                onChangeVirtualHosts(next);
                                            }}
                                        />
                                        <Input
                                            className="font-mono"
                                            placeholder="/"
                                            data-vhost-context-path
                                            pattern={proxyContextPathInputPattern}
                                            title={proxyContextPathInputTitle}
                                            aria-invalid={Boolean(virtualHostsError)}
                                            value={row.path}
                                            onChange={(e) => {
                                                const next = [...virtualHosts];
                                                next[idx] = { ...next[idx], path: e.target.value };
                                                onChangeVirtualHosts(next);
                                            }}
                                        />
                                        <div className="flex items-center justify-between gap-3">
                                            <div className="flex items-center gap-2">
                                                <Switch
                                                    checked={row.overrideAccess}
                                                    onCheckedChange={(checked) => {
                                                        const next = [...virtualHosts];
                                                        next[idx] = { ...next[idx], overrideAccess: Boolean(checked) };
                                                        onChangeVirtualHosts(next);
                                                    }}
                                                />
                                                <span className="text-xs text-muted-foreground">Enable</span>
                                            </div>

                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                className="size-8 text-muted-foreground hover:text-destructive disabled:opacity-30"
                                                onClick={() => {
                                                    const next = virtualHosts.length <= 1 ? virtualHosts : virtualHosts.filter((_, i) => i !== idx);
                                                    onChangeVirtualHosts(next.length ? next : [{ host: '', path: '/', overrideAccess: false }]);
                                                }}
                                                disabled={virtualHosts.length <= 1}
                                                aria-label="Remove virtual host"
                                            >
                                                <Trash2Icon className="size-4" aria-hidden="true" />
                                            </Button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => onChangeVirtualHosts([...virtualHosts, { host: '', path: '/', overrideAccess: false }])}
                        >
                            <PlusIcon className="mr-1.5 size-4" aria-hidden="true" />
                            Add context-path
                        </Button>

                        {virtualHostsError ? <p className="text-xs text-destructive">{virtualHostsError}</p> : null}
                        {pathVerifyPending ? (
                            <p className="text-xs text-muted-foreground">Checking virtual hosts…</p>
                        ) : null}
                        {serverPathError && enableVirtualHosts ? (
                            <p className="text-xs text-destructive">{serverPathError}</p>
                        ) : null}
                            </div>
                        )}
                    </div>
                </div>
            </Card>

            <Card className="rounded-xl">
                <CardHeader className="space-y-1 p-4 pb-3 sm:p-6 sm:pb-4">
                    <CardTitle className="flex items-center gap-2">
                        <Zap className="size-4 text-primary" aria-hidden="true" />
                        Upstream URL
                    </CardTitle>
                    <CardDescription>Where the gateway forwards traffic to.</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4 p-4 pt-0 sm:p-6 sm:pt-0">
                    <div className="space-y-2">
                        <Label htmlFor="target-url">
                            Target URL <span className="text-destructive">*</span>
                        </Label>
                        <div className="relative">
                            <Globe
                                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                                aria-hidden="true"
                            />
                            <Input
                                ref={targetUrlInputRef}
                                id="target-url"
                                type="url"
                                className="font-mono"
                                style={{ paddingLeft: '2.75rem' }}
                                placeholder="https://api.internal:8443/v1"
                                aria-invalid={Boolean(targetUrlError)}
                                value={targetUrl}
                                onChange={(e) => onChangeTargetUrl(e.target.value)}
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">
                            The backend URL that the gateway forwards traffic to (e.g. https://api.internal:8443/v1).
                        </p>
                        {targetUrlError ? <p className="text-xs text-destructive">{targetUrlError}</p> : null}
                    </div>

                    <div className="flex items-start gap-3 rounded-lg border border-amber-200/60 bg-amber-50/60 p-3">
                        <Sparkles className="mt-0.5 size-4 shrink-0 text-amber-700" aria-hidden="true" />
                        <p className="text-xs leading-relaxed">
                            <span className="font-semibold text-foreground">Tip: use internal DNS for upstreams.</span>{' '}
                            <span className="text-muted-foreground">It keeps upstreams stable across environments and avoids hard-coding IPs.</span>
                        </p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

export function CreateProxyWizard({ onExit }: Readonly<{ onExit: () => void }>) {
    const navigate = useNavigate();
    const targetUrlInputRef = useRef<HTMLInputElement>(null);
    const contextPathInputRef = useRef<HTMLInputElement>(null);
    const configureProxyRootRef = useRef<HTMLDivElement>(null);
    const prevApiNameForContextPathRef = useRef<string | null>(null);
    const {
        actions: { getValue, nextStep, previousStep, setStep, updateField, validateActiveStep },
        selectors: { activeStep, activeStepIndex },
        state,
    } = useApiCreationStore();

    useEffect(() => {
        const name = state.data.details.name;
        const trimmed = name.trim();
        const desired = trimmed ? `/${trimmed}` : '';
        const current = state.data.proxy.contextPath.trim();
        const prevName = prevApiNameForContextPathRef.current ?? '';
        const prevTrimmed = prevName.trim();
        const prevDesired = prevTrimmed ? `/${prevTrimmed}` : '';
        const stillDerivedFromName = current === '' || current === prevDesired;

        if (stillDerivedFromName && current !== desired) {
            updateField('proxy.contextPath', desired);
        }
        prevApiNameForContextPathRef.current = name;
    }, [state.data.details.name, updateField]);

    const { data: bootstrap, isLoading: bootstrapLoading, error: bootstrapError } = useProxyConnectorBootstrap(true);
    const createProxy = useCreateProxyMutation();
    const pathVerify = useDebouncedVerifyPaths(state.data.proxy, activeStep?.id === 'configure-proxy');
    const serverPathReason =
        pathVerify.data && pathVerify.data.ok === false && pathVerify.data.reason ? pathVerify.data.reason : undefined;

    const isReview = activeStep?.id === 'review-deploy';

    const canGoBack = activeStepIndex > 0;
    const canGoNext = activeStepIndex < state.steps.length - 1;

    const footerLabel = useMemo(() => {
        return footerActionLabel(activeStep?.id, activeStep?.label, state.data.deployImmediately);
    }, [activeStep?.id, activeStep?.label, state.data.deployImmediately]);

    const handleNext = useCallback(() => {
        if (!activeStep) return;
        if (!validateActiveStep()) return;
        if (activeStep.id === 'configure-proxy') {
            const { enableVirtualHosts } = state.data.proxy;
            if (!enableVirtualHosts) {
                const cp = contextPathInputRef.current;
                if (cp && !cp.checkValidity()) {
                    cp.reportValidity();
                    return;
                }
            } else {
                const root = configureProxyRootRef.current;
                for (const el of Array.from(root?.querySelectorAll<HTMLInputElement>('[data-vhost-context-path]') ?? [])) {
                    if (!el.checkValidity()) {
                        el.reportValidity();
                        return;
                    }
                }
            }
            const el = targetUrlInputRef.current;
            if (el && !el.checkValidity()) {
                el.reportValidity();
                return;
            }
            if (pathVerify.isFetching || pathVerify.isPending) return;
            if (serverPathReason) return;
        }
        nextStep();
    }, [
        activeStep,
        nextStep,
        pathVerify.isFetching,
        pathVerify.isPending,
        serverPathReason,
        state.data.proxy,
        validateActiveStep,
    ]);

    const handleDeploy = useCallback(() => {
        if (!validateActiveStep()) return;
        if (!bootstrap) return;
        createProxy.mutate(
            { data: state.data, bootstrap },
            {
                onSuccess: (result: CreateProxyWorkflowResult) => {
                    navigate(`../${encodeURIComponent(result.api.id)}`, {
                        relative: 'path',
                        state: {
                            deployed: result.deployed,
                            warnings: result.warnings,
                        },
                    });
                },
            },
        );
    }, [bootstrap, createProxy, navigate, state.data, validateActiveStep]);

    const stepper = useMemo(() => {
        const iconForStep = (stepId: string) => {
            switch (stepId) {
                case 'api-details':
                    return FileTextIcon;
                case 'configure-proxy':
                    return GlobeIcon;
                case 'secure':
                    return ShieldIcon;
                case 'review-deploy':
                    return ScrollTextIcon;
                default:
                    return CircleCheckIcon;
            }
        };

        return (
            <div className="rounded-xl border bg-card p-1">
                <div className="flex items-center">
                    {state.steps.map((step, idx) => {
                        const isActive = step.id === state.activeStepId;
                        const isDone = idx < activeStepIndex;
                        const canClick = idx <= activeStepIndex;
                        const StepIcon = isDone ? CircleCheckIcon : iconForStep(step.id);

                        return (
                            <div key={step.id} className="flex flex-1 items-center">
                                <button
                                    type="button"
                                    onClick={() => (canClick ? setStep(step.id) : undefined)}
                                    disabled={!canClick}
                                    className={`flex w-full items-center justify-center gap-2.5 rounded-lg px-3 py-2.5 text-sm transition-colors ${
                                        isActive
                                            ? 'bg-primary/10 text-primary font-medium'
                                            : isDone
                                              ? 'text-success hover:bg-muted/50 cursor-pointer'
                                              : 'text-muted-foreground'
                                    }`}
                                >
                                    <span
                                        className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-medium ${
                                            isActive
                                                ? 'bg-primary text-primary-foreground'
                                                : isDone
                                                  ? 'bg-success/15 text-success'
                                                  : 'bg-muted text-muted-foreground'
                                        }`}
                                    >
                                        <StepIcon className="size-3.5" aria-hidden="true" />
                                    </span>
                                    <span className="min-w-0 truncate">{step.label}</span>
                                </button>
                                {idx < state.steps.length - 1 ? <div className={`h-px w-6 shrink-0 ${isDone ? 'bg-success/40' : 'bg-border'}`} /> : null}
                            </div>
                        );
                    })}
                </div>
            </div>
        );
    }, [activeStepIndex, setStep, state.activeStepId, state.steps]);

    return (
        <div className="w-full max-w-none space-y-6 min-w-0">
            <div className="flex w-full flex-col gap-3 sm:flex-row sm:items-start sm:gap-4">
                <div className="min-w-0 space-y-2">
                    <h1 className="text-2xl font-semibold tracking-tight">Create API Proxy</h1>
                    <p className="text-sm text-muted-foreground">Set up a secure, managed proxy in four simple steps.</p>
                </div>
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="ml-auto h-9 shrink-0 gap-2 self-end sm:self-start"
                    asChild
                >
                    <a href={DOCS_URL} target="_blank" rel="noopener noreferrer">
                        <ScrollTextIcon className="size-4" aria-hidden="true" />
                        Documentation
                    </a>
                </Button>
            </div>

            {stepper}

            {isReview && bootstrapError ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                    Could not load entrypoint plugins: {getApimErrorMessage(bootstrapError)}. Check Management API URL and organization
                    environment (override with <code className="text-xs">window.__GAMMA_APIM_RUNTIME__</code> in dev).
                </Card>
            ) : null}

            {isReview && createProxy.isError ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                    {getApimErrorMessage(createProxy.error)}
                </Card>
            ) : null}

            <div className="flex flex-col gap-6 md:flex-row md:items-start">
                <main className="min-w-0 flex-1 space-y-6">
                    <Card className="rounded-xl p-4 sm:p-6">
                        {activeStep ? (
                            activeStep.id === 'review-deploy' ? (
                                <ReviewRenderer
                                    steps={state.steps}
                                    state={state.data}
                                    onEditStep={setStep}
                                    onChangeDeployImmediately={(next) => updateField('deployImmediately', next)}
                                />
                            ) : activeStep.id === 'configure-proxy' ? (
                                <ConfigureProxyStepContent
                                    contextPath={state.data.proxy.contextPath}
                                    enableVirtualHosts={state.data.proxy.enableVirtualHosts}
                                    targetUrl={state.data.proxy.targetUrl}
                                    contextPathInputRef={contextPathInputRef}
                                    configureProxyRootRef={configureProxyRootRef}
                                    targetUrlInputRef={targetUrlInputRef}
                                    virtualHosts={state.data.proxy.virtualHosts}
                                    errors={state.validationErrors}
                                    serverPathError={serverPathReason}
                                    pathVerifyPending={pathVerify.isFetching || pathVerify.isPending}
                                    onChangeContextPath={(v) => updateField('proxy.contextPath', v)}
                                    onChangeEnableVirtualHosts={(v) => updateField('proxy.enableVirtualHosts', v)}
                                    onChangeTargetUrl={(v) => updateField('proxy.targetUrl', v)}
                                    onChangeVirtualHosts={(v) => updateField('proxy.virtualHosts', v)}
                                />
                            ) : (
                                <FormRenderer
                                    fields={activeStep.fields}
                                    state={state.data}
                                    getValue={getValue}
                                    updateField={updateField}
                                    errors={state.validationErrors}
                                />
                            )
                        ) : null}
                    </Card>
                </main>

                <aside className="min-w-0 md:sticky md:top-6 md:w-96 md:shrink-0 md:self-start" aria-label="Request path through the proxy">
                        <RequestPathCard
                            stepId={activeStep?.id}
                            contextPath={state.data.proxy.contextPath}
                            enableVirtualHosts={state.data.proxy.enableVirtualHosts}
                            virtualHosts={state.data.proxy.virtualHosts}
                            targetUrl={state.data.proxy.targetUrl}
                            securityType={state.data.security.type}
                        />
                </aside>
            </div>

            <div className="flex w-full flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center">
                <div className="flex items-center gap-2">
                    <Button type="button" variant="outline" size="sm" onClick={onExit}>
                        Back to templates
                    </Button>
                    <Button type="button" variant="outline" size="sm" disabled={!canGoBack} onClick={previousStep}>
                        Previous
                    </Button>
                </div>

                <div className="flex flex-1 items-center justify-end gap-3">
                    {activeStep ? (
                        <span className="text-xs text-muted-foreground">
                            Step {activeStepIndex + 1} of {state.steps.length}
                        </span>
                    ) : null}
                    <Button
                        type="button"
                        size="sm"
                        disabled={
                            (!canGoNext && !isReview) ||
                            (activeStep?.id === 'configure-proxy' &&
                                (pathVerify.isFetching || pathVerify.isPending || Boolean(serverPathReason))) ||
                            (isReview && (bootstrapLoading || !bootstrap || Boolean(bootstrapError) || createProxy.isPending))
                        }
                        onClick={isReview ? handleDeploy : handleNext}
                    >
                        {isReview && createProxy.isPending ? 'Creating…' : footerLabel}
                        <ArrowRightIcon className="ml-2 size-4" aria-hidden="true" />
                    </Button>
                </div>
            </div>
        </div>
    );
}

export function WizardStarter({ templateId, onExit }: Readonly<{ templateId?: string; onExit: () => void }>) {
    const {
        actions: { startScratch, startTemplate },
    } = useApiCreationStore();

    useEffect(() => {
        if (templateId) startTemplate(templateId);
        else startScratch();
    }, [startScratch, startTemplate, templateId]);

    return <CreateProxyWizard onExit={onExit} />;
}

