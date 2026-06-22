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
import { Alert, AlertDescription, Button, PageFocused, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { PlanFormStepIndicator, buildWizardSteps } from './PlanFormStepIndicator';
import { PlanGeneralStep } from './PlanGeneralStep';
import { PlanRestrictionsStep } from './PlanRestrictionsStep';
import { PlanSecurityStep } from './PlanSecurityStep';
import { useCreatePlan, usePlan, useUpdatePlan } from '../../../../hooks/usePlans';
import type {
    GeneralFormData,
    PlanContext,
    PlanFormValue,
    PlanSecurityType,
    RestrictionsFormData,
    SecurityFormData,
} from '../../../../types/plan';
import { EMPTY_GENERAL, EMPTY_RESTRICTIONS, EMPTY_SECURITY, PLAN_SECURITY_LABELS } from '../../../../types/plan';
import { planToFormValue } from '../../../../utils/planTransformers';

function validateGeneral(data: GeneralFormData): Partial<Record<keyof GeneralFormData, string>> {
    const errors: Partial<Record<keyof GeneralFormData, string>> = {};
    if (!data.name.trim()) errors.name = 'Name is required.';
    else if (data.name.trim().length > 50) errors.name = 'Name must be at most 50 characters.';
    return errors;
}

interface PlanFormWizardProps {
    ctx: PlanContext;
    securityType: PlanSecurityType;
    planId?: string;
    readOnly?: boolean;
    /** Security and Restrictions steps are shown read-only (published/deprecated plans). */
    securityLocked?: boolean;
}

export function PlanFormWizard({ ctx, securityType, planId, readOnly = false, securityLocked = false }: Readonly<PlanFormWizardProps>) {
    const navigate = useNavigate();
    const isEdit = Boolean(planId);

    const { data: existingPlan, isLoading: isLoadingPlan } = usePlan(ctx, planId);

    const [form, setForm] = useState<PlanFormValue>({
        securityType,
        general: { ...EMPTY_GENERAL },
        security: { ...EMPTY_SECURITY },
        restrictions: { ...EMPTY_RESTRICTIONS },
    });
    const [stepIndex, setStepIndex] = useState(0);
    const [generalErrors, setGeneralErrors] = useState<Partial<Record<keyof GeneralFormData, string>>>({});
    const initialized = useRef(false);

    useEffect(() => {
        if (existingPlan && !initialized.current) {
            setForm(planToFormValue(existingPlan));
            initialized.current = true;
        }
    }, [existingPlan]);

    const createMutation = useCreatePlan(ctx);
    const updateMutation = useUpdatePlan(ctx);
    const mutationError = (createMutation.error ?? updateMutation.error)?.message ?? null;
    const isPending = createMutation.isPending || updateMutation.isPending;

    const steps = buildWizardSteps(securityType, ctx.type, stepIndex);
    const totalSteps = steps.length;

    const handleNext = () => {
        if (!readOnly && stepIndex === 0) {
            const errors = validateGeneral(form.general);
            if (Object.keys(errors).length > 0) {
                setGeneralErrors(errors);
                return;
            }
            setGeneralErrors({});
        }
        setStepIndex(prev => Math.min(prev + 1, totalSteps - 1));
    };

    const handleBack = () => setStepIndex(prev => Math.max(prev - 1, 0));

    const handleSubmit = () => {
        if (isEdit && planId) {
            updateMutation.mutate({ planId, form }, { onSuccess: () => navigate('..') });
        } else {
            createMutation.mutate(form, { onSuccess: () => navigate('..') });
        }
    };

    if (isEdit && isLoadingPlan) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-12 w-full rounded" />
                <Skeleton className="h-64 w-full rounded" />
            </div>
        );
    }

    // Current step label for determining which component to render
    const currentStepLabel = steps[stepIndex]?.label;

    const isSecurityStep = currentStepLabel === 'Security';
    const isRestrictionsStep = currentStepLabel === 'Restrictions';
    const isGeneralStep = currentStepLabel === 'General';
    const isLastStep = stepIndex === totalSteps - 1;

    return (
        <PageFocused>
            <div className="flex flex-col gap-6">
                {/* Header */}
                <div className="flex items-center gap-3">
                    <Button type="button" variant="ghost" size="icon" onClick={() => navigate('..')}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        <span className="sr-only">Back to plans</span>
                    </Button>
                    <div>
                        <h1 className="text-2xl font-semibold tracking-tight">
                            {readOnly ? 'View plan' : isEdit ? 'Edit plan' : `Create plan`}
                        </h1>
                        <p className="text-sm text-muted-foreground">
                            {readOnly ? 'View' : isEdit ? 'Edit' : 'New'} {PLAN_SECURITY_LABELS[form.securityType]} plan
                        </p>
                    </div>
                </div>

                {securityLocked && (
                    <Alert>
                        <AlertDescription>
                            Security and restrictions settings are read-only for published or deprecated plans. Only general settings can be
                            updated.
                        </AlertDescription>
                    </Alert>
                )}

                {/* Step indicator */}
                <PlanFormStepIndicator steps={steps} />

                {/* Step content */}
                {isGeneralStep && (
                    <PlanGeneralStep
                        ctx={ctx}
                        securityType={securityType}
                        value={form.general}
                        onChange={(general: GeneralFormData) => setForm({ ...form, general })}
                        errors={generalErrors}
                        readOnly={readOnly}
                    />
                )}

                {isSecurityStep && (
                    <PlanSecurityStep
                        ctx={ctx}
                        securityType={securityType}
                        value={form.security}
                        onChange={(security: SecurityFormData) => setForm({ ...form, security })}
                        readOnly={readOnly || securityLocked}
                    />
                )}

                {isRestrictionsStep && (
                    <PlanRestrictionsStep
                        value={form.restrictions}
                        onChange={(restrictions: RestrictionsFormData) => setForm({ ...form, restrictions })}
                        readOnly={readOnly || securityLocked}
                    />
                )}

                {/* Mutation error */}
                {mutationError && (
                    <Alert variant="destructive">
                        <AlertDescription>{mutationError}</AlertDescription>
                    </Alert>
                )}

                {/* Navigation */}
                <div className="flex items-center justify-between pt-2">
                    <Button type="button" variant="outline" onClick={stepIndex === 0 ? () => navigate('..') : handleBack}>
                        {stepIndex === 0 ? 'Cancel' : 'Previous'}
                    </Button>

                    {readOnly ? (
                        isLastStep ? (
                            <Button type="button" variant="outline" onClick={() => navigate('..')}>
                                Close
                            </Button>
                        ) : (
                            <Button type="button" onClick={handleNext}>
                                Next
                            </Button>
                        )
                    ) : (
                        <Button type="button" onClick={isLastStep ? handleSubmit : handleNext} disabled={isPending}>
                            {isLastStep ? (isPending ? 'Saving…' : isEdit ? 'Save changes' : 'Create plan') : 'Next'}
                        </Button>
                    )}
                </div>
            </div>
        </PageFocused>
    );
}
