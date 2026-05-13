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
import type { LucideIcon } from '@gravitee/graphene-core/icons';

export interface StepConfig {
    label: string;
    Icon: LucideIcon;
}

interface StepProgressProps {
    steps: StepConfig[];
    activeStep: number;
}

export function StepProgress({ steps, activeStep }: StepProgressProps) {
    return (
        <nav aria-label="Creation steps" className="flex items-center">
            {steps.map((step, index) => {
                const isDone = index < activeStep;
                const isActive = index === activeStep;
                const Icon = step.Icon;

                return (
                    <div key={step.label} className="flex flex-1 items-center" aria-current={isActive ? 'step' : undefined}>
                        <div
                            className={cn(
                                'flex w-full items-center justify-center rounded-lg px-3 py-2 text-sm transition-colors',
                                isActive && 'font-medium',
                            )}
                            style={{ gap: '0.625rem' }}
                        >
                            {/* Icon badge */}
                            <span
                                className={cn(
                                    'flex shrink-0 items-center justify-center rounded-full text-xs',
                                    isActive && 'bg-primary text-primary-foreground',
                                    !isActive && !isDone && 'bg-muted text-muted-foreground',
                                )}
                                style={{
                                    width: '1.5rem',
                                    height: '1.5rem',
                                    ...(isDone ? { backgroundColor: 'rgba(34,197,94,0.1)', color: 'var(--color-success)' } : {}),
                                }}
                            >
                                {isDone ? (
                                    <CircleCheckIcon style={{ width: '0.875rem', height: '0.875rem' }} aria-hidden />
                                ) : (
                                    <Icon style={{ width: '0.875rem', height: '0.875rem' }} aria-hidden />
                                )}
                            </span>

                            {/* Label */}
                            <span
                                className={cn('whitespace-nowrap text-sm', !isActive && !isDone && 'text-muted-foreground')}
                                style={
                                    isActive ? { color: 'var(--color-foreground)' } : isDone ? { color: 'var(--color-success)' } : undefined
                                }
                            >
                                {step.label}
                            </span>
                        </div>

                        {/* Connector */}
                        {index < steps.length - 1 && (
                            <div
                                className="h-px w-6 shrink-0"
                                style={{
                                    backgroundColor: isDone ? 'var(--color-success)' : 'var(--color-border)',
                                    opacity: isDone ? 0.5 : 1,
                                }}
                            />
                        )}
                    </div>
                );
            })}
        </nav>
    );
}
