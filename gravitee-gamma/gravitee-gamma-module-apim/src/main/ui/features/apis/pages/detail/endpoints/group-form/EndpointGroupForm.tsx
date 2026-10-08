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
import { WizardStepIndicator } from '../../../../components/WizardStepIndicator';
import type { HealthCheckConfigFormState, HealthCheckFormState } from '../../../../utils/healthCheckForm';
import { validateHealthCheckForm } from '../../../../utils/healthCheckForm';
import type { EndpointConfigurationFormState, EndpointGroupFormState, SharedConfigFormState } from '../types';
import { validateGroupName } from '../types';

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
    /** Create flow: Configuration step collects default endpoint configuration + group shared config. */
    isCreateMode?: boolean;
    isTcp?: boolean;
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
    isTcp = false,
    isReadOnly = false,
    isSaving,
    saveError,
    onSave,
    onCancel,
}: Readonly<EndpointGroupFormProps>) {
    const endpointType = isTcp ? 'tcp-proxy' : 'http-proxy';
    const steps = useMemo(() => (showHealthCheck ? [...BASE_STEPS, HEALTH_CHECK_STEP] : [...BASE_STEPS]), [showHealthCheck]);

    const [currentStep, setCurrentStep] = useState<StepId>(initialStep);
    const [form, setForm] = useState<EndpointGroupFormState>(initialForm);
    const [healthCheckErrors, setHealthCheckErrors] = useState<Record<string, string>>({});
    const [sharedConfigValid, setSharedConfigValid] = useState(false);
    const [endpointConfigurationValid, setEndpointConfigurationValid] = useState(false);

    function patchForm(patch: Partial<EndpointGroupFormState>) {
        setForm(prev => ({ ...prev, ...patch }));
    }

    function setSharedConfig(next: SharedConfigFormState) {
        setForm(prev => ({ ...prev, sharedConfig: next }));
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

    const generalValid = !nameError && form.name.trim().length > 0;
    const createConfigurationValid = !isCreateMode || endpointConfigurationValid;
    const configurationValid = sharedConfigValid && createConfigurationValid;
    const healthCheckValid = !showHealthCheck || Object.keys(validateHealthCheckForm(form.healthCheck)).length === 0;

    const currentStepIndex = steps.findIndex(s => s.id === currentStep);
    const isLastStep = currentStepIndex === steps.length - 1;
    const canGoBack = currentStepIndex > 0;

    function goNext() {
        if (currentStep === 'general' && !generalValid) return;
        if (currentStep === 'configuration' && !configurationValid) return;
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
        if (!configurationValid) {
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
                <div hidden={currentStep !== 'general'}>
                    <GeneralStep form={form} existingGroupNames={existingGroupNames} onFormChange={patchForm} readOnly={isReadOnly} />
                </div>
                <div hidden={currentStep !== 'configuration'}>
                    <ConfigurationStep
                        config={form.sharedConfig}
                        onChange={setSharedConfig}
                        onSharedConfigValidityChange={setSharedConfigValid}
                        showDefaultEndpointConfiguration={isCreateMode}
                        defaultEndpointConfiguration={form.defaultEndpointConfiguration ?? {}}
                        onDefaultEndpointConfigurationChange={(next: EndpointConfigurationFormState) =>
                            patchForm({ defaultEndpointConfiguration: next })
                        }
                        onEndpointConfigurationValidityChange={setEndpointConfigurationValid}
                        endpointType={endpointType}
                        disabled={isReadOnly}
                    />
                </div>
                {showHealthCheck && (
                    <div hidden={currentStep !== 'health-check'}>
                        <HealthCheckStep
                            mode="group"
                            healthCheck={form.healthCheck}
                            errors={healthCheckErrors}
                            readOnly={isReadOnly}
                            onChange={patchHealthCheck}
                            onConfigChange={patchHealthCheckConfig}
                        />
                    </div>
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
                            disabled={
                                isReadOnly || isSaving || !generalValid || !configurationValid || (showHealthCheck && !healthCheckValid)
                            }
                        >
                            {isSaving ? 'Saving…' : 'Save endpoint group'}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
