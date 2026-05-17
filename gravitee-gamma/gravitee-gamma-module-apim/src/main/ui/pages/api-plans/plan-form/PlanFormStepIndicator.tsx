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
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

export interface WizardStep {
    label: string;
    state: 'active' | 'completed' | 'upcoming';
}

interface PlanFormStepIndicatorProps {
    steps: WizardStep[];
}

export function PlanFormStepIndicator({ steps }: Readonly<PlanFormStepIndicatorProps>) {
    return (
        <Card>
            <CardContent className="py-3 px-4">
                <nav aria-label="Plan creation steps" className="flex items-center">
                    {steps.map((step, idx) => (
                        <div key={step.label} className="flex flex-1 items-center">
                            <div
                                className="flex w-full items-center justify-center rounded-lg px-3 py-2 text-sm"
                                style={{ gap: '0.625rem' }}
                            >
                                {/* Step badge */}
                                <span
                                    className={cn(
                                        'flex shrink-0 items-center justify-center rounded-full text-xs font-semibold',
                                        step.state === 'active' && 'bg-primary text-primary-foreground',
                                        step.state === 'upcoming' && 'bg-muted text-muted-foreground',
                                    )}
                                    style={{
                                        width: '1.5rem',
                                        height: '1.5rem',
                                        ...(step.state === 'completed'
                                            ? { backgroundColor: 'rgba(34,197,94,0.1)', color: 'var(--color-success)' }
                                            : {}),
                                    }}
                                    aria-current={step.state === 'active' ? 'step' : undefined}
                                >
                                    {step.state === 'completed' ? (
                                        <CircleCheckIcon style={{ width: '0.875rem', height: '0.875rem' }} aria-hidden />
                                    ) : (
                                        <span>{idx + 1}</span>
                                    )}
                                </span>

                                {/* Label */}
                                <span
                                    className={cn(
                                        'whitespace-nowrap text-sm font-medium',
                                        step.state === 'upcoming' && 'text-muted-foreground',
                                    )}
                                    style={
                                        step.state === 'active'
                                            ? { color: 'var(--color-foreground)' }
                                            : step.state === 'completed'
                                              ? { color: 'var(--color-success)' }
                                              : undefined
                                    }
                                >
                                    {step.label}
                                </span>
                            </div>

                            {/* Connector */}
                            {idx < steps.length - 1 && (
                                <div
                                    className="h-px w-8 shrink-0"
                                    style={{
                                        backgroundColor: step.state === 'completed' ? 'var(--color-success)' : 'var(--color-border)',
                                        opacity: step.state === 'completed' ? 0.5 : 1,
                                    }}
                                />
                            )}
                        </div>
                    ))}
                </nav>
            </CardContent>
        </Card>
    );
}

/** Derive the WizardStep array dynamically based on security type and context. */
export function buildWizardSteps(securityType: string, ctxType: 'api' | 'api-product', currentStepIndex: number): WizardStep[] {
    const defs: string[] = ['General'];
    if (securityType !== 'KEY_LESS') defs.push('Security');
    if (ctxType === 'api') defs.push('Restrictions');

    return defs.map((label, i) => ({
        label,
        state: i < currentStepIndex ? 'completed' : i === currentStepIndex ? 'active' : 'upcoming',
    }));
}
