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
import { CircleCheckIcon, FileTextIcon, GlobeIcon, InfoIcon, ScrollTextIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import { Fragment } from 'react';
import type React from 'react';

import type { StepId } from '../../../../utils/schema';
import type { StepConfig } from '../../../../utils/stepRegistry';

type WizardStepperProps = Readonly<{
    steps: readonly StepConfig[];
    activeStepId: StepId | undefined;
    activeStepIndex: number;
    onStepClick: (stepId: StepId) => void;
}>;

const STEP_ICONS: Record<string, React.ComponentType<React.SVGProps<SVGSVGElement>>> = {
    essentials: InfoIcon,
    'api-details': FileTextIcon,
    'configure-proxy': GlobeIcon,
    secure: ShieldIcon,
    'review-deploy': ScrollTextIcon,
};

export function WizardStepper({ steps, activeStepId, activeStepIndex, onStepClick }: WizardStepperProps) {
    return (
        <div className="rounded-xl border bg-card p-1">
            <div className="flex items-center">
                {steps.map((step, idx) => {
                    const isActive = step.id === activeStepId;
                    const isDone = idx < activeStepIndex;
                    const canClick = isDone;
                    const StepIcon = isDone ? CircleCheckIcon : (STEP_ICONS[step.id] ?? CircleCheckIcon);

                    return (
                        <Fragment key={step.id}>
                            <button
                                type="button"
                                onClick={() => (canClick ? onStepClick(step.id as StepId) : undefined)}
                                disabled={!canClick && !isActive}
                                className={`flex flex-1 items-center justify-center gap-2.5 rounded-lg px-3 py-2.5 text-sm transition-colors ${
                                    isActive
                                        ? 'bg-primary/10 font-medium text-primary'
                                        : isDone
                                          ? 'cursor-pointer text-success hover:bg-muted/50'
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
                            {idx < steps.length - 1 ? (
                                <div
                                    aria-hidden="true"
                                    className="shrink-0 w-8"
                                    style={{
                                        height: '2px',
                                        borderRadius: '9999px',
                                        backgroundColor: isDone ? 'var(--color-success)' : 'currentColor',
                                        opacity: isDone ? 0.45 : 0.2,
                                    }}
                                />
                            ) : null}
                        </Fragment>
                    );
                })}
            </div>
        </div>
    );
}
