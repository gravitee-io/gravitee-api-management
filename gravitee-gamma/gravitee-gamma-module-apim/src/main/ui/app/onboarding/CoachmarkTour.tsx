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
import { Button, Popover, PopoverAnchor, PopoverContent, PopoverDescription, PopoverTitle, cn } from '@gravitee/graphene-core';
import { ChevronLeftIcon, ChevronRightIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState } from 'react';
import type * as React from 'react';

import type { CoachmarkStep } from './tours';

/** The highlighted sidebar item, marked by graphene's `SidebarMenuButton` when `isActive`. */
const ACTIVE_NAV_SELECTOR = '[data-slot="sidebar-menu-button"][data-active="true"]';
/** Gap between the highlighted nav item and the coachmark, in px (Radix `sideOffset`). */
const NAV_SIDE_OFFSET = 12;
/** Minimum distance the coachmark keeps from the viewport edges, in px (Radix `collisionPadding`). */
const VIEWPORT_COLLISION_PADDING = 16;

/** Minimal shape Radix needs to position against an element it does not own (`Measurable`). */
type Measurable = { getBoundingClientRect: () => DOMRect };

/** Identity rect used when no nav item is highlighted (e.g. collapsed sidebar). */
const EMPTY_RECT: DOMRect = { x: 0, y: 0, width: 0, height: 0, top: 0, right: 0, bottom: 0, left: 0, toJSON: () => ({}) };

/**
 * A stable virtual anchor that always resolves to the currently-highlighted sidebar item. Handing it
 * to Radix (via graphene's `PopoverAnchor`) lets the popover own positioning, collision handling, and
 * scroll/resize tracking — so the coachmark points at the section each step describes without any
 * manual measurement. Resolves lazily on every measure, so it follows the highlight as steps change.
 */
function useActiveNavAnchor(): React.RefObject<Measurable> {
    const anchorRef = useRef<Measurable>({
        getBoundingClientRect: () => document.querySelector(ACTIVE_NAV_SELECTOR)?.getBoundingClientRect() ?? EMPTY_RECT,
    });
    return anchorRef;
}

export interface CoachmarkTourProps {
    open: boolean;
    steps: readonly CoachmarkStep[];
    onComplete: () => void;
    onSkip: () => void;
    onStepShown?: (step: CoachmarkStep) => void;
}

export function CoachmarkTour({ open, steps, onComplete, onSkip, onStepShown }: Readonly<CoachmarkTourProps>) {
    const [currentStep, setCurrentStep] = useState(0);
    const navAnchorRef = useActiveNavAnchor();

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

    const handleNext = () => (isLastStep ? onComplete() : goToStep(stepIndex + 1));
    const handleBack = () => {
        if (!isFirstStep) {
            goToStep(stepIndex - 1);
        }
    };

    return (
        // Non-modal so the app stays interactive; Radix routes Escape through `onOpenChange`.
        <Popover open modal={false} onOpenChange={isOpen => !isOpen && onSkip()}>
            <PopoverAnchor virtualRef={navAnchorRef} />
            <PopoverContent
                side="right"
                align="center"
                sideOffset={NAV_SIDE_OFFSET}
                collisionPadding={VIEWPORT_COLLISION_PADDING}
                // Keep the coachmark pinned while the user reads — only the tour controls dismiss it.
                onInteractOutside={event => event.preventDefault()}
                aria-label={step.title}
                className="w-96 max-w-[var(--radix-popover-content-available-width)] gap-0 p-5"
            >
                <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <div className="shrink-0 rounded-xl bg-primary/10 p-2.5">
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

                <PopoverTitle className="mt-3 text-base font-semibold tracking-tight">{step.title}</PopoverTitle>
                <PopoverDescription className="mt-1.5 text-sm leading-relaxed">{step.description}</PopoverDescription>

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
            </PopoverContent>
        </Popover>
    );
}
