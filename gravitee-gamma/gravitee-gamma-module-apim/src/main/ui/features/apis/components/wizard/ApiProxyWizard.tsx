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
import { Alert, AlertDescription, Button, Separator } from '@gravitee/graphene-core';
import {
    ArrowLeftIcon,
    ArrowRightIcon,
    FileTextIcon,
    GlobeIcon,
    Loader2Icon,
    RocketIcon,
    ShieldIcon,
    ZapIcon,
} from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { ProxyFlowVisualization } from './ProxyFlowVisualization';
import type { StepConfig } from './StepProgress';
import { StepProgress } from './StepProgress';
import { useCreateApiProxy } from '../../hooks/useCreateApiProxy';
import { useApiCreation } from '../../store/apiCreationStore';
import type { ApiProxyDraft, ValidationErrors } from '../../types/apiCreation';
import { validateDetails, validateEntrypoints, validateEssentials, validateSecurity } from '../../utils/apiCreationValidation';
import { DetailsStep } from '../steps/DetailsStep';
import { EntrypointsStep } from '../steps/EntrypointsStep';
import { EssentialsStep } from '../steps/EssentialsStep';
import { ReviewDeployStep } from '../steps/ReviewDeployStep';
import { SecurityStep } from '../steps/SecurityStep';

// ─── Step configuration ───────────────────────────────────────────────────────

const SCRATCH_STEPS: StepConfig[] = [
    { label: 'API Details', Icon: FileTextIcon },
    { label: 'Configure Proxy', Icon: GlobeIcon },
    { label: 'Secure', Icon: ShieldIcon },
    { label: 'Review & Deploy', Icon: RocketIcon },
];

const TEMPLATE_STEPS: StepConfig[] = [
    { label: 'Essentials', Icon: ZapIcon },
    { label: 'Review & Deploy', Icon: RocketIcon },
];

const SCRATCH_CONTENT: Record<number, ComponentType> = {
    0: DetailsStep,
    1: EntrypointsStep,
    2: SecurityStep,
    3: ReviewDeployStep,
};

const TEMPLATE_CONTENT: Record<number, ComponentType> = {
    0: EssentialsStep,
    1: ReviewDeployStep,
};

const SCRATCH_VALIDATORS: Record<number, (form: ApiProxyDraft) => ValidationErrors> = {
    0: validateDetails,
    1: validateEntrypoints,
    2: validateSecurity,
};

const TEMPLATE_VALIDATORS: Record<number, (form: ApiProxyDraft) => ValidationErrors> = {
    0: validateEssentials,
};

// ─── Component ────────────────────────────────────────────────────────────────

interface ApiProxyWizardProps {
    mode: 'scratch' | 'template';
}

export function ApiProxyWizard({ mode }: ApiProxyWizardProps) {
    const navigate = useNavigate();
    const { state, dispatch } = useApiCreation();
    const { mutate, isPending, error: createError, isSuccess } = useCreateApiProxy();

    const steps = mode === 'scratch' ? SCRATCH_STEPS : TEMPLATE_STEPS;
    const contentMap = mode === 'scratch' ? SCRATCH_CONTENT : TEMPLATE_CONTENT;
    const validatorMap = mode === 'scratch' ? SCRATCH_VALIDATORS : TEMPLATE_VALIDATORS;
    const activeStep = state.step;
    const isLastStep = activeStep === steps.length - 1;

    const StepContent = contentMap[activeStep];

    useEffect(() => {
        if (isSuccess) navigate('../..');
    }, [isSuccess, navigate]);

    function handleNext() {
        const validator = validatorMap[activeStep];
        if (validator) {
            const errors = validator(state.form);
            if (Object.keys(errors).length > 0) {
                dispatch({ type: 'SET_VALIDATION_ERRORS', errors });
                return;
            }
        }
        dispatch({ type: 'CLEAR_VALIDATION_ERRORS' });
        dispatch({ type: 'SET_STEP', step: activeStep + 1 });
    }

    function handleBack() {
        if (activeStep === 0) {
            navigate('..');
        } else {
            dispatch({ type: 'CLEAR_VALIDATION_ERRORS' });
            dispatch({ type: 'SET_STEP', step: activeStep - 1 });
        }
    }

    function handleCreate() {
        mutate(state.form);
    }

    const hasErrors = Object.keys(state.validationErrors).length > 0;
    const isBusy = isPending || state.isPathVerifying;

    return (
        <div className="space-y-4">
            {/* Full-width step progress — compact height */}
            <div className="rounded-xl border bg-card px-4 py-2">
                <StepProgress steps={steps} activeStep={activeStep} />
            </div>

            {/* Two-column layout: step content left, flow visualization right */}
            <div className="grid gap-6 items-start" style={{ gridTemplateColumns: '1fr min(20rem, 36vw)' }}>
                <div className="space-y-4">
                    <div className="rounded-xl border bg-card p-6">{StepContent && <StepContent />}</div>

                    {createError && (
                        <Alert variant="destructive">
                            <AlertDescription>{createError.message}</AlertDescription>
                        </Alert>
                    )}
                </div>

                <aside aria-label="Request path through the proxy">
                    <ProxyFlowVisualization mode={mode} />
                </aside>
            </div>

            <Separator />

            <div className="flex items-center justify-between">
                <Button variant="outline" onClick={handleBack} disabled={isBusy}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    {activeStep === 0 ? 'Cancel' : 'Back'}
                </Button>

                {isLastStep ? (
                    <Button onClick={handleCreate} disabled={isBusy || hasErrors}>
                        {isPending ? (
                            <Loader2Icon className="size-4 animate-spin" aria-hidden />
                        ) : (
                            <RocketIcon className="size-4" aria-hidden />
                        )}
                        {state.form.deployImmediately ? 'Create & Deploy' : 'Create API'}
                    </Button>
                ) : (
                    <Button onClick={handleNext} disabled={isBusy || hasErrors}>
                        <ArrowRightIcon className="size-4" aria-hidden />
                        Next
                    </Button>
                )}
            </div>
        </div>
    );
}
