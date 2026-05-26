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
import { useState } from 'react';

import { ConfigurationStep } from './ConfigurationStep';
import { GeneralStep } from './GeneralStep';
import type { EndpointGroupFormState, SharedConfigFormState } from '../types';
import { validateGroupName } from '../types';
import { WizardStepIndicator } from '../WizardStepIndicator';

const STEPS = [
    { id: 'general', label: 'General' },
    { id: 'configuration', label: 'Configuration' },
] as const;

type StepId = (typeof STEPS)[number]['id'];

interface EndpointGroupFormProps {
    initialForm: EndpointGroupFormState;
    existingGroupNames: string[];
    isSaving: boolean;
    saveError: string | null;
    onSave: (form: EndpointGroupFormState) => void;
    onCancel: () => void;
}

export function EndpointGroupForm({
    initialForm,
    existingGroupNames,
    isSaving,
    saveError,
    onSave,
    onCancel,
}: Readonly<EndpointGroupFormProps>) {
    const [currentStep, setCurrentStep] = useState<StepId>('general');
    const [form, setForm] = useState<EndpointGroupFormState>(initialForm);

    function patchForm(patch: Partial<EndpointGroupFormState>) {
        setForm(prev => ({ ...prev, ...patch }));
    }

    function patchSharedConfig(patch: Partial<SharedConfigFormState>) {
        setForm(prev => ({ ...prev, sharedConfig: { ...prev.sharedConfig, ...patch } }));
    }

    const nameError = (() => {
        const base = validateGroupName(form.name);
        if (base) return base;
        const lower = form.name.toLowerCase();
        if (existingGroupNames.some(n => n.toLowerCase() === lower)) return 'Name must be unique.';
        return null;
    })();

    const generalValid = !nameError && form.name.trim().length > 0;

    const currentStepIndex = STEPS.findIndex(s => s.id === currentStep);
    const isLastStep = currentStep === 'configuration';
    const canGoBack = currentStepIndex > 0;

    function goNext() {
        const next = STEPS[currentStepIndex + 1];
        if (next) setCurrentStep(next.id);
    }

    function goBack() {
        const prev = STEPS[currentStepIndex - 1];
        if (prev) setCurrentStep(prev.id);
    }

    return (
        <div className="space-y-6">
            {/* ── Stepper indicator ── */}
            <WizardStepIndicator steps={STEPS} currentStepId={currentStep} onStepClick={id => setCurrentStep(id as StepId)} />

            {/* ── Step content ── */}
            {saveError && (
                <Alert variant="destructive">
                    <AlertDescription>{saveError}</AlertDescription>
                </Alert>
            )}

            <div>
                {currentStep === 'general' && <GeneralStep form={form} existingGroupNames={existingGroupNames} onFormChange={patchForm} />}
                {currentStep === 'configuration' && <ConfigurationStep config={form.sharedConfig} onChange={patchSharedConfig} />}
            </div>

            {/* ── Navigation ── */}
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
                        <Button type="button" size="sm" onClick={goNext} disabled={!generalValid}>
                            Next
                        </Button>
                    ) : (
                        <Button type="button" size="sm" onClick={() => onSave(form)} disabled={isSaving || !generalValid}>
                            {isSaving ? 'Saving…' : 'Save endpoint group'}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
