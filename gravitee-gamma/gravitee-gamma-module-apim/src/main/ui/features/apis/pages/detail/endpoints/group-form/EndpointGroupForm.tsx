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
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import { ConfigurationStep } from './ConfigurationStep';
import { GeneralStep } from './GeneralStep';
import { HealthCheckStep } from './HealthCheckStep';
import { validateHttpProxyOptions } from '../../../../utils/endpointSharedConfiguration';
import type { HealthCheckConfigFormState, HealthCheckFormState } from '../../../../utils/healthCheckForm';
import { validateHealthCheckForm } from '../../../../utils/healthCheckForm';
import type { EndpointGroupFormState, SharedConfigFormState } from '../types';
import { validateEndpointTarget, validateGroupName } from '../types';
import { WizardStepIndicator } from '../WizardStepIndicator';

const BASE_STEPS = [
    { id: 'general', label: 'General' },
    { id: 'configuration', label: 'Configuration' },
] as const;

const HEALTH_CHECK_STEP = { id: 'health-check', label: 'Health-check' } as const;

type BaseStepId = (typeof BASE_STEPS)[number]['id'];
type StepId = BaseStepId | 'health-check';

interface EndpointGroupFormProps {
    initialForm: EndpointGroupFormState;
    existingGroupNames: string[];
    initialStep?: StepId;
    showHealthCheck?: boolean;
    /** Create flow: Configuration step collects default endpoint target + group shared config. */
    isCreateMode?: boolean;
    isReadOnly?: boolean;
    isSaving: boolean;
    saveError: string | null;
    onSave: (form: EndpointGroupFormState) => void;
    onCancel: () => void;
}

export function EndpointGroupForm({
    initialForm,
    existingGroupNames,
    initialStep = 'general',
    showHealthCheck = false,
    isCreateMode = false,
    isReadOnly = false,
    isSaving,
    saveError,
    onSave,
    onCancel,
}: Readonly<EndpointGroupFormProps>) {
    const steps = useMemo(() => (showHealthCheck ? [...BASE_STEPS, HEALTH_CHECK_STEP] : [...BASE_STEPS]), [showHealthCheck]);

    const [currentStep, setCurrentStep] = useState<StepId>(initialStep);
    const [form, setForm] = useState<EndpointGroupFormState>(initialForm);
    const [healthCheckErrors, setHealthCheckErrors] = useState<Record<string, string>>({});
    const [proxyError, setProxyError] = useState<string | null>(null);
    const [targetError, setTargetError] = useState<string | null>(null);

    function patchForm(patch: Partial<EndpointGroupFormState>) {
        setForm(prev => ({ ...prev, ...patch }));
    }

    function patchSharedConfig(patch: Partial<SharedConfigFormState>) {
        setForm(prev => ({ ...prev, sharedConfig: { ...prev.sharedConfig, ...patch } }));
        setProxyError(null);
    }

    function patchHealthCheck(patch: Partial<HealthCheckFormState>) {
        setForm(prev => ({ ...prev, healthCheck: { ...prev.healthCheck, ...patch } }));
        setHealthCheckErrors({});
    }

    function patchHealthCheckConfig(patch: Partial<HealthCheckConfigFormState>) {
        setForm(prev => ({
            ...prev,
            healthCheck: {
                ...prev.healthCheck,
                configuration: { ...prev.healthCheck.configuration, ...patch },
            },
        }));
        setHealthCheckErrors({});
    }

    const nameError = (() => {
        const base = validateGroupName(form.name);
        if (base) return base;
        const lower = form.name.trim().toLowerCase();
        if (existingGroupNames.some(n => n.trim().toLowerCase() === lower)) return 'Name must be unique.';
        return null;
    })();

    const endpointTargetError = isCreateMode ? validateEndpointTarget(form.defaultEndpointTarget ?? '') : null;

    const generalValid = !nameError && form.name.trim().length > 0;
    const configurationValid =
        validateHttpProxyOptions(form.sharedConfig.proxy) === null && (!isCreateMode || endpointTargetError === null);
    const healthCheckValid = !showHealthCheck || Object.keys(validateHealthCheckForm(form.healthCheck)).length === 0;

    const currentStepIndex = steps.findIndex(s => s.id === currentStep);
    const isLastStep = currentStepIndex === steps.length - 1;
    const canGoBack = currentStepIndex > 0;

    function goNext() {
        if (currentStep === 'general' && !generalValid) return;
        if (currentStep === 'configuration') {
            const targetErr = isCreateMode ? validateEndpointTarget(form.defaultEndpointTarget ?? '') : null;
            setTargetError(targetErr);
            const proxyErr = validateHttpProxyOptions(form.sharedConfig.proxy);
            setProxyError(proxyErr);
            if (targetErr || proxyErr) return;
        }
        if (currentStep === 'health-check') {
            const errors = validateHealthCheckForm(form.healthCheck);
            setHealthCheckErrors(errors);
            if (Object.keys(errors).length > 0) return;
        }
        const next = steps[currentStepIndex + 1];
        if (next) setCurrentStep(next.id);
    }

    function goBack() {
        const prev = steps[currentStepIndex - 1];
        if (prev) setCurrentStep(prev.id);
    }

    function handleSave() {
        const targetErr = isCreateMode ? validateEndpointTarget(form.defaultEndpointTarget ?? '') : null;
        setTargetError(targetErr);
        const proxyErr = validateHttpProxyOptions(form.sharedConfig.proxy);
        setProxyError(proxyErr);
        if (targetErr) {
            setCurrentStep('configuration');
            return;
        }
        if (proxyErr) {
            setCurrentStep('configuration');
            return;
        }
        if (showHealthCheck) {
            const errors = validateHealthCheckForm(form.healthCheck);
            setHealthCheckErrors(errors);
            if (Object.keys(errors).length > 0) {
                setCurrentStep('health-check');
                return;
            }
        }
        onSave(form);
    }

    const nextDisabled =
        (currentStep === 'general' && !generalValid) ||
        (currentStep === 'configuration' && !configurationValid) ||
        (currentStep === 'health-check' && !healthCheckValid);

    return (
        <div className="space-y-6">
            <WizardStepIndicator steps={steps} currentStepId={currentStep} onStepClick={id => setCurrentStep(id as StepId)} />

            {saveError && (
                <Alert variant="destructive">
                    <AlertDescription>{saveError}</AlertDescription>
                </Alert>
            )}

            <div>
                {currentStep === 'general' && <GeneralStep form={form} existingGroupNames={existingGroupNames} onFormChange={patchForm} />}
                {currentStep === 'configuration' && (
                    <ConfigurationStep
                        config={form.sharedConfig}
                        proxyError={proxyError}
                        onChange={patchSharedConfig}
                        showDefaultEndpointTarget={isCreateMode}
                        defaultEndpointTarget={form.defaultEndpointTarget ?? ''}
                        targetError={targetError}
                        onTargetChange={value => {
                            patchForm({ defaultEndpointTarget: value });
                            setTargetError(null);
                        }}
                    />
                )}
                {currentStep === 'health-check' && showHealthCheck && (
                    <HealthCheckStep
                        mode="group"
                        healthCheck={form.healthCheck}
                        errors={healthCheckErrors}
                        readOnly={isReadOnly}
                        onChange={patchHealthCheck}
                        onConfigChange={patchHealthCheckConfig}
                    />
                )}
            </div>

            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    {canGoBack && (
                        <Button type="button" size="sm" variant="outline" onClick={goBack}>
                            Back
                        </Button>
                    )}
                    <Button type="button" size="sm" variant="outline" onClick={onCancel}>
                        Cancel
                    </Button>
                </div>

                <div className="flex items-center gap-2">
                    {!isLastStep ? (
                        <Button type="button" size="sm" onClick={goNext} disabled={nextDisabled}>
                            {currentStep === 'general' ? 'Validate general information' : 'Next'}
                        </Button>
                    ) : (
                        <Button
                            type="button"
                            size="sm"
                            onClick={handleSave}
                            disabled={isSaving || !generalValid || !configurationValid || (showHealthCheck && !healthCheckValid)}
                        >
                            {isSaving ? 'Saving…' : 'Save endpoint group'}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
