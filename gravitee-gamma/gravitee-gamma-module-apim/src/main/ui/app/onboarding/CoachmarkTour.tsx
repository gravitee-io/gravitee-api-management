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
import { Button, Card, cn, usePortalContainer } from '@gravitee/graphene-core';
import { ChevronLeftIcon, ChevronRightIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState } from 'react';
import { createPortal as renderInOverlay } from 'react-dom';

import type { CoachmarkStep } from './tours';

const useOverlayContainer: () => HTMLElement | undefined = typeof usePortalContainer === 'function' ? usePortalContainer : () => undefined;

export interface CoachmarkTourProps {
    open: boolean;
    steps: readonly CoachmarkStep[];
    onComplete: () => void;
    onSkip: () => void;
    onStepShown?: (step: CoachmarkStep) => void;
}

export function CoachmarkTour({ open, steps, onComplete, onSkip, onStepShown }: Readonly<CoachmarkTourProps>) {
    const [currentStep, setCurrentStep] = useState(0);
    const portalContainer = useOverlayContainer();

    const onStepShownRef = useRef(onStepShown);
    onStepShownRef.current = onStepShown;

    useEffect(() => {
        if (open) {
            setCurrentStep(0);
            if (steps.length > 0) {
                onStepShownRef.current?.(steps[0]);
            }
        }
    }, [open, steps]);

    const onSkipRef = useRef(onSkip);
    onSkipRef.current = onSkip;
    useEffect(() => {
        if (!open) {
            return;
        }
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onSkipRef.current();
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [open]);

    if (!open || steps.length === 0) {
        return null;
    }

    const stepIndex = Math.min(currentStep, steps.length - 1);
    const step = steps[stepIndex];
    const isLastStep = stepIndex === steps.length - 1;
    const isFirstStep = stepIndex === 0;
    const StepIcon = step.icon;

    const goToStep = (index: number) => {
        setCurrentStep(index);
        onStepShownRef.current?.(steps[index]);
    };

    const handleNext = () => {
        if (isLastStep) {
            onComplete();
        } else {
            goToStep(stepIndex + 1);
        }
    };

    const handleBack = () => {
        if (!isFirstStep) {
            goToStep(stepIndex - 1);
        }
    };

    const card = (
        <Card
            role="complementary"
            aria-label={step.title}
            className="p-0"
            style={{
                position: 'fixed',
                bottom: 24,
                left: 24,
                zIndex: 1000,
                width: 380,
                maxWidth: 'calc(100vw - 48px)',
            }}
        >
            <div className="p-5">
                <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <div className="rounded-xl bg-primary/10 p-2.5 shrink-0">
                            <StepIcon className="size-5 text-primary" aria-hidden />
                        </div>
                        <span className="text-xs font-medium text-muted-foreground">
                            Step {stepIndex + 1} of {steps.length}
                        </span>
                    </div>
                    <Button type="button" variant="ghost" size="icon" className="size-7 shrink-0" onClick={onSkip} aria-label="Close tour">
                        <XIcon className="size-4" aria-hidden />
                    </Button>
                </div>

                <h2 className="mt-3 text-base font-semibold tracking-tight">{step.title}</h2>
                <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">{step.description}</p>

                <div className="mt-5 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-1.5" aria-hidden>
                        {steps.map((s, index) => (
                            <span
                                key={s.id}
                                className={cn('size-1.5 rounded-full transition-colors', index === stepIndex ? 'bg-primary' : 'bg-border')}
                            />
                        ))}
                    </div>

                    <div className="flex items-center gap-2">
                        <Button type="button" variant="ghost" size="sm" onClick={onSkip}>
                            Skip tour
                        </Button>
                        {!isFirstStep ? (
                            <Button type="button" variant="outline" size="sm" onClick={handleBack}>
                                <ChevronLeftIcon className="size-4" aria-hidden />
                                Back
                            </Button>
                        ) : null}
                        <Button type="button" size="sm" onClick={handleNext}>
                            {isLastStep ? 'Done' : 'Next'}
                            {isLastStep ? null : <ChevronRightIcon className="size-4" aria-hidden />}
                        </Button>
                    </div>
                </div>
            </div>
        </Card>
    );

    return portalContainer ? renderInOverlay(card, portalContainer) : card;
}
