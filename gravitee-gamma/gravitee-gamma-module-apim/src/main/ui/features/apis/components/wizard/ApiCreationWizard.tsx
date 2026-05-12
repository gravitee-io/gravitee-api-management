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
import { AlertCircleIcon, CircleCheckIcon, ScrollTextIcon } from '@gravitee/graphene-core/icons';
import type React from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { RequestPathPreview } from './RequestPathPreview';
import { WizardFooter } from './WizardFooter';
import { WizardStepper } from './WizardStepper';
import type { CreateProxyWorkflowResult } from '../../../../utils/createProxyWorkflow';
import type { StepId } from '../../../../utils/schema';
import { getTemplateById } from '../../../../utils/stepRegistry';
import { getApimErrorMessage, useCreateProxyMutation } from '../../hooks/useCreateProxyMutation';
import { useDebouncedVerifyPaths } from '../../hooks/useDebouncedVerifyPaths';
import { useProxyConnectorBootstrap } from '../../hooks/useProxyConnectorBootstrap';
import { useApiCreationStore } from '../../store/useApiCreationStore';
import { formatSecurityType } from '../../utils/securityFormatters';
import { ApiDetailsStep } from '../steps/ApiDetailsStep';
import { ConfigureProxyStep } from '../steps/ConfigureProxyStep';
import type { ConfigureProxyStepRef } from '../steps/ConfigureProxyStep';
import { ReviewDeployStep } from '../steps/ReviewDeployStep';
import { SecureStep } from '../steps/SecureStep';
import { TemplateEssentialsStep } from '../steps/TemplateEssentialsStep';
import { TemplateReviewStep } from '../steps/TemplateReviewStep';

const DOCS_URL = 'https://documentation.gravitee.io/apim';

type ApiCreationWizardProps = Readonly<{ onExit: () => void }>;

export function ApiCreationWizard({ onExit }: ApiCreationWizardProps) {
    const navigate = useNavigate();
    const configureProxyStepRef = useRef<ConfigureProxyStepRef>(null);
    const prevApiNameRef = useRef<string | null>(null);
    const [creationResult, setCreationResult] = useState<{ apiId: string; deployed: boolean; warnings: string[] } | null>(null);

    const {
        actions: { nextStep, previousStep, setStep, updateField, validateActiveStep },
        selectors: { activeStep, activeStepIndex },
        state,
    } = useApiCreationStore();

    const { data: bootstrap, isLoading: bootstrapLoading, error: bootstrapError } = useProxyConnectorBootstrap(true);
    const createProxy = useCreateProxyMutation();
    const pathVerify = useDebouncedVerifyPaths(state.data.proxy, activeStep?.id === 'configure-proxy' || activeStep?.id === 'essentials');
    const serverPathReason = pathVerify.data?.ok === false && pathVerify.data?.reason ? pathVerify.data.reason : undefined;

    const isReview = activeStep?.id === 'review-deploy';
    const template = state.mode === 'template' ? getTemplateById(state.templateId) : undefined;

    useEffect(() => {
        const name = state.data.details.name;
        const trimmed = name.trim();
        const desired = trimmed ? `/${trimmed}` : '';
        const current = state.data.proxy.contextPath.trim();
        const prevName = prevApiNameRef.current ?? '';
        const prevDesired = prevName.trim() ? `/${prevName.trim()}` : '';
        const stillDerived = current === '' || current === prevDesired;

        if (stillDerived && current !== desired) {
            updateField('proxy.contextPath', desired);
        }
        prevApiNameRef.current = name;
    }, [state.data.details.name, updateField]);

    const handleNext = useCallback(() => {
        if (!activeStep) return;
        if (!validateActiveStep()) return;
        if (activeStep.id === 'configure-proxy' || activeStep.id === 'essentials') {
            if (activeStep.id === 'configure-proxy') {
                if (!configureProxyStepRef.current?.validate()) return;
            }
            if (pathVerify.isFetching || pathVerify.isPending) return;
            if (serverPathReason) return;
        }
        nextStep();
    }, [activeStep, nextStep, pathVerify.isFetching, pathVerify.isPending, serverPathReason, validateActiveStep]);

    const handleDeploy = useCallback(() => {
        if (!validateActiveStep()) return;
        if (!bootstrap) return;
        createProxy.mutate(
            { data: state.data, bootstrap },
            {
                onSuccess: (result: CreateProxyWorkflowResult) => {
                    if (result.warnings.length > 0) {
                        setCreationResult({ apiId: result.api.id, deployed: result.deployed, warnings: result.warnings });
                    } else {
                        navigate(`../${encodeURIComponent(result.api.id)}/overview`, {
                            relative: 'path',
                            state: { deployed: result.deployed },
                        });
                    }
                },
            },
        );
    }, [bootstrap, createProxy, navigate, state.data, validateActiveStep]);

    const isPathStep = activeStep?.id === 'configure-proxy' || activeStep?.id === 'essentials';
    const pathVerifyBlocking = isPathStep && (pathVerify.isFetching || pathVerify.isPending || Boolean(serverPathReason));
    const deployBlocking =
        isReview && (bootstrapLoading || !bootstrap || Boolean(bootstrapError) || createProxy.isPending || Boolean(creationResult));
    const isPrimaryDisabled = pathVerifyBlocking || deployBlocking;

    return (
        <div className="w-full max-w-none space-y-6">
            <div className="flex w-full flex-col gap-3 sm:flex-row sm:items-start sm:gap-4">
                <div className="min-w-0 space-y-2">
                    <h1 className="text-2xl font-semibold tracking-tight">Create API Proxy</h1>
                    <p className="text-sm text-muted-foreground">
                        {template ? `Using template: ${template.label}` : 'Set up a secure, managed proxy in a few simple steps.'}
                    </p>
                </div>
                <Button type="button" variant="outline" size="sm" className="ml-auto h-9 shrink-0 gap-2 self-end sm:self-start" asChild>
                    <a href={DOCS_URL} target="_blank" rel="noopener noreferrer">
                        <ScrollTextIcon className="size-4" aria-hidden="true" />
                        Documentation
                    </a>
                </Button>
            </div>

            {/* Template active banner — shows what's pre-configured */}
            {template ? (
                <div className="flex flex-wrap items-center gap-2 rounded-lg border border-primary/20 bg-primary/5 px-4 py-2.5">
                    <CircleCheckIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
                    <span className="text-sm font-medium text-primary">{template.label}</span>
                    <span className="text-xs text-muted-foreground">— security pre-configured:</span>
                    <Badge variant="secondary" className="border border-primary/20 bg-primary/10 text-primary text-xs">
                        {formatSecurityType(state.data.security.type)}
                    </Badge>
                    <span className="text-xs text-muted-foreground">· Customize in Review if needed</span>
                </div>
            ) : null}

            <WizardStepper steps={state.steps} activeStepId={state.activeStepId} activeStepIndex={activeStepIndex} onStepClick={setStep} />

            {isReview && bootstrapError ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                    Could not load entrypoint plugins: {getApimErrorMessage(bootstrapError)}. Check Management API URL and organization
                    environment.
                </Card>
            ) : null}

            {isReview && createProxy.isError ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                    {getApimErrorMessage(createProxy.error)}
                </Card>
            ) : null}

            {creationResult ? (
                <Card className="rounded-xl border border-yellow-500/30 bg-yellow-500/5 p-4">
                    <div className="flex items-start gap-3">
                        <AlertCircleIcon className="mt-0.5 size-4 shrink-0 text-yellow-600" aria-hidden="true" />
                        <div className="min-w-0 flex-1 space-y-2">
                            <p className="text-sm font-semibold text-foreground">API created — some steps completed with warnings</p>
                            <ul className="space-y-1">
                                {creationResult.warnings.map((w, i) => (
                                    <li key={i} className="text-xs text-muted-foreground">
                                        {w}
                                    </li>
                                ))}
                            </ul>
                            <Button
                                type="button"
                                size="sm"
                                onClick={() =>
                                    navigate(`../${encodeURIComponent(creationResult.apiId)}/overview`, {
                                        relative: 'path',
                                        state: { deployed: creationResult.deployed },
                                    })
                                }
                            >
                                Continue to API
                            </Button>
                        </div>
                    </div>
                </Card>
            ) : null}

            <div className="flex items-start gap-6">
                <main className="min-w-0 flex-1 overflow-x-clip">
                    <Card className="rounded-xl p-4 sm:p-6">
                        <StepContent
                            activeStepId={activeStep?.id}
                            state={state}
                            template={template}
                            configureProxyStepRef={configureProxyStepRef}
                            pathVerify={pathVerify}
                            serverPathReason={serverPathReason}
                            setStep={setStep}
                            updateField={updateField}
                        />
                    </Card>
                </main>

                <aside className="w-80 shrink-0 sticky top-4 self-start" aria-label="Configuration preview">
                    <RequestPathPreview stepId={activeStep?.id} proxy={state.data.proxy} securityType={state.data.security.type} />
                </aside>
            </div>

            <WizardFooter
                activeStep={activeStep}
                activeStepIndex={activeStepIndex}
                totalSteps={state.steps.length}
                isReview={isReview}
                deployImmediately={state.data.deployImmediately}
                canGoBack={activeStepIndex > 0}
                isPrimaryDisabled={isPrimaryDisabled}
                isPending={createProxy.isPending}
                isTemplate={Boolean(template)}
                onBack={onExit}
                onPrevious={previousStep}
                onPrimary={isReview ? handleDeploy : handleNext}
            />
        </div>
    );
}

type StepContentProps = Readonly<{
    activeStepId: string | undefined;
    state: ReturnType<typeof useApiCreationStore>['state'];
    template: ReturnType<typeof getTemplateById>;
    configureProxyStepRef: React.RefObject<ConfigureProxyStepRef | null>;
    pathVerify: ReturnType<typeof useDebouncedVerifyPaths>;
    serverPathReason: string | undefined;
    setStep: (stepId: StepId) => void;
    updateField: (path: string, value: unknown) => void;
}>;

function StepContent({
    activeStepId,
    state,
    template,
    configureProxyStepRef,
    pathVerify,
    serverPathReason,
    setStep,
    updateField,
}: StepContentProps) {
    switch (activeStepId) {
        case 'essentials':
            return (
                <TemplateEssentialsStep
                    data={state.data}
                    errors={state.validationErrors}
                    templateLabel={template?.label ?? 'template'}
                    updateField={updateField}
                />
            );

        case 'api-details':
            return <ApiDetailsStep details={state.data.details} errors={state.validationErrors} updateField={updateField} />;

        case 'configure-proxy':
            return (
                <ConfigureProxyStep
                    ref={configureProxyStepRef}
                    proxy={state.data.proxy}
                    errors={state.validationErrors}
                    serverPathError={serverPathReason}
                    pathVerifyPending={pathVerify.isFetching || pathVerify.isPending}
                    updateField={updateField}
                />
            );

        case 'secure':
            return (
                <SecureStep
                    security={state.data.security}
                    errors={state.validationErrors}
                    getValue={path => {
                        const parts = path.split('.');
                        let cur: any = state.data;
                        for (const p of parts) cur = cur?.[p];
                        return cur;
                    }}
                    updateField={updateField}
                />
            );

        case 'review-deploy':
            if (state.mode === 'template') {
                return (
                    <TemplateReviewStep
                        state={state.data}
                        templateLabel={template?.label ?? 'template'}
                        onEditEssentials={() => setStep('essentials')}
                        onChangeDeployImmediately={next => updateField('deployImmediately', next)}
                        errors={state.validationErrors}
                        getValue={path => {
                            const parts = path.split('.');
                            let cur: any = state.data;
                            for (const p of parts) cur = cur?.[p];
                            return cur;
                        }}
                        updateField={updateField}
                    />
                );
            }
            return (
                <ReviewDeployStep
                    steps={state.steps}
                    state={state.data}
                    onEditStep={setStep}
                    onChangeDeployImmediately={next => updateField('deployImmediately', next)}
                />
            );

        default:
            return null;
    }
}

export function WizardStarter({ templateId, onExit }: Readonly<{ templateId?: string; onExit: () => void }>) {
    const {
        actions: { startScratch, startTemplate },
    } = useApiCreationStore();

    useEffect(() => {
        if (templateId) startTemplate(templateId);
        else startScratch();
    }, [startScratch, startTemplate, templateId]);

    return <ApiCreationWizard onExit={onExit} />;
}
