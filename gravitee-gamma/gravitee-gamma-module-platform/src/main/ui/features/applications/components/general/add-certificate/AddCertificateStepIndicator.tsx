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
import { cn } from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

const STEP_LABELS = ['Upload', 'Configure', 'Confirm'] as const;

export function buildAddCertificateSteps(stepIndex: number): { label: string; state: 'active' | 'completed' | 'upcoming' }[] {
    return STEP_LABELS.map((label, index) => ({
        label,
        state: index < stepIndex ? 'completed' : index === stepIndex ? 'active' : 'upcoming',
    }));
}

export function AddCertificateStepIndicator({ stepIndex }: Readonly<{ stepIndex: number }>) {
    const steps = buildAddCertificateSteps(stepIndex);

    return (
        <nav aria-label="Add certificate steps" className="flex items-center justify-between gap-2">
            {steps.map((step, index) => (
                <div key={step.label} className="flex flex-1 items-center">
                    <div className="flex w-full flex-col items-center gap-1.5 px-2 py-1 text-center sm:flex-row sm:justify-center sm:gap-2.5">
                        <span
                            className={cn(
                                'flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold',
                                step.state === 'active' && 'bg-primary text-primary-foreground',
                                step.state === 'upcoming' && 'bg-muted text-muted-foreground',
                                step.state === 'completed' && 'bg-primary/10 text-primary',
                            )}
                            aria-current={step.state === 'active' ? 'step' : undefined}
                        >
                            {step.state === 'completed' ? <CircleCheckIcon className="size-3.5" aria-hidden /> : index + 1}
                        </span>
                        <span
                            className={cn(
                                'text-xs font-medium sm:text-sm',
                                step.state === 'upcoming' && 'text-muted-foreground',
                                step.state === 'completed' && 'text-primary',
                            )}
                        >
                            {step.label}
                        </span>
                    </div>
                    {index < steps.length - 1 ? (
                        <div
                            className={cn('hidden h-px w-6 shrink-0 sm:block', step.state === 'completed' ? 'bg-primary/40' : 'bg-border')}
                            aria-hidden
                        />
                    ) : null}
                </div>
            ))}
        </nav>
    );
}
