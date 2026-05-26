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
import { Card, CardContent } from '@gravitee/graphene-core';
import { CheckIcon, FlaskConicalIcon } from '@gravitee/graphene-core/icons';

export interface WizardStep {
    id: string;
    label: string;
    comingSoon?: boolean;
}

interface WizardStepIndicatorProps {
    steps: readonly WizardStep[];
    currentStepId: string;
    onStepClick: (stepId: string) => void;
    ariaLabel?: string;
}

export function WizardStepIndicator({ steps, currentStepId, onStepClick, ariaLabel = 'Form steps' }: Readonly<WizardStepIndicatorProps>) {
    const currentStepIndex = steps.findIndex(s => s.id === currentStepId);

    return (
        <Card>
            <CardContent className="py-4">
                <nav aria-label={ariaLabel}>
                    <ol className="flex items-center">
                        {steps.map((step, idx) => {
                            const isActive = step.id === currentStepId;
                            const isCompleted = idx < currentStepIndex;
                            return (
                                <li key={step.id} className="flex flex-1 justify-center items-center">
                                    <button
                                        type="button"
                                        className={`flex items-center gap-2 text-sm transition-colors${isActive ? ' font-semibold text-foreground' : isCompleted ? ' text-primary cursor-pointer' : ' text-muted-foreground cursor-default'}`}
                                        onClick={isCompleted ? () => onStepClick(step.id) : undefined}
                                        disabled={!isCompleted && !isActive}
                                        aria-current={isActive ? 'step' : undefined}
                                    >
                                        <span
                                            className={`flex size-6 items-center justify-center rounded-full text-xs font-semibold shrink-0 ${
                                                isCompleted || isActive
                                                    ? 'bg-primary text-primary-foreground'
                                                    : 'border border-muted-foreground/30 text-muted-foreground'
                                            }`}
                                        >
                                            {isCompleted ? <CheckIcon className="size-3.5" aria-hidden /> : idx + 1}
                                        </span>
                                        <span>{step.label}</span>
                                        {step.comingSoon && (
                                            <FlaskConicalIcon
                                                className="size-3.5 text-muted-foreground/60 shrink-0"
                                                aria-label="Coming soon"
                                            />
                                        )}
                                    </button>
                                </li>
                            );
                        })}
                    </ol>
                </nav>
            </CardContent>
        </Card>
    );
}
