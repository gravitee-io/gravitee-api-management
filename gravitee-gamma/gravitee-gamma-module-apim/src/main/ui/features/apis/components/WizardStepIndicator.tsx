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
import { Card, CardContent, cn } from '@gravitee/graphene-core';
import { CircleCheckIcon, FlaskConicalIcon } from '@gravitee/graphene-core/icons';

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

/**
 * Shared wizard progress indicator. Matches the API creation and Plans wizards:
 * a card-wrapped row of numbered steps with connector lines and a green/success completed state.
 * Completed steps are clickable to navigate back; other steps stay in the tab order as disabled buttons.
 */
export function WizardStepIndicator({ steps, currentStepId, onStepClick, ariaLabel = 'Form steps' }: Readonly<WizardStepIndicatorProps>) {
    const currentStepIndex = steps.findIndex(s => s.id === currentStepId);

    return (
        <Card>
            <CardContent className="py-3 px-4">
                <nav aria-label={ariaLabel}>
                    <ol className="flex items-center">
                        {steps.map((step, idx) => {
                            const isActive = step.id === currentStepId;
                            const isCompleted = idx < currentStepIndex;

                            return (
                                <li key={step.id} className="flex flex-1 items-center">
                                    <button
                                        type="button"
                                        className={cn(
                                            'flex w-full items-center justify-center gap-2.5 rounded-lg px-3 py-2 text-sm',
                                            isCompleted ? 'cursor-pointer' : 'cursor-default',
                                        )}
                                        onClick={isCompleted ? () => onStepClick(step.id) : undefined}
                                        disabled={!isCompleted && !isActive}
                                        aria-current={isActive ? 'step' : undefined}
                                    >
                                        <span
                                            className={cn(
                                                'flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold',
                                                isActive
                                                    ? 'bg-primary text-primary-foreground'
                                                    : isCompleted
                                                      ? 'bg-success/10 text-success'
                                                      : 'bg-muted text-muted-foreground',
                                            )}
                                        >
                                            {isCompleted ? <CircleCheckIcon className="size-3.5" aria-hidden /> : <span>{idx + 1}</span>}
                                        </span>

                                        <span
                                            className={cn(
                                                'whitespace-nowrap text-sm font-medium',
                                                isActive ? 'text-foreground' : isCompleted ? 'text-success' : 'text-muted-foreground',
                                            )}
                                        >
                                            {step.label}
                                        </span>

                                        {step.comingSoon && (
                                            <FlaskConicalIcon
                                                className="size-3.5 text-muted-foreground/60 shrink-0"
                                                aria-label="Coming soon"
                                            />
                                        )}
                                    </button>

                                    {idx < steps.length - 1 && (
                                        <div className={cn('h-px w-8 shrink-0', isCompleted ? 'bg-success/50' : 'bg-border')} />
                                    )}
                                </li>
                            );
                        })}
                    </ol>
                </nav>
            </CardContent>
        </Card>
    );
}
