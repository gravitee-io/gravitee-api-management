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
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';

import { CoachmarkTour } from './CoachmarkTour';
import { APIM_OVERVIEW_TOUR_ID, TOURS } from './tours';
import type { RouteKey } from '../../config/routes';

const ONBOARDING_STORAGE_KEY = 'gravitee-apim-onboarding';

interface OnboardingState {
    readonly dismissedTours: string[];
}

const EMPTY_STATE: OnboardingState = { dismissedTours: [] };

function isOnboardingState(value: unknown): value is OnboardingState {
    return (
        typeof value === 'object' &&
        value !== null &&
        Array.isArray((value as OnboardingState).dismissedTours) &&
        (value as OnboardingState).dismissedTours.every(id => typeof id === 'string')
    );
}

function loadOnboardingState(): OnboardingState {
    try {
        const raw = globalThis.localStorage?.getItem(ONBOARDING_STORAGE_KEY);
        if (!raw) {
            return EMPTY_STATE;
        }
        const parsed: unknown = JSON.parse(raw);
        return isOnboardingState(parsed) ? parsed : EMPTY_STATE;
    } catch {
        return EMPTY_STATE;
    }
}

function saveOnboardingState(state: OnboardingState): void {
    try {
        globalThis.localStorage?.setItem(ONBOARDING_STORAGE_KEY, JSON.stringify(state));
    } catch {
        return;
    }
}

export interface OnboardingContextValue {
    readonly activeTourId: string | null;
    isTourDismissed: (tourId: string) => boolean;
    openTour: (tourId: string) => void;
    dismissTour: (tourId: string) => void;
}

const OnboardingContext = createContext<OnboardingContextValue | null>(null);

export function OnboardingProvider({ children }: { children: ReactNode }) {
    const [dismissedTours, setDismissedTours] = useState<ReadonlySet<string>>(() => new Set(loadOnboardingState().dismissedTours));
    const [activeTourId, setActiveTourId] = useState<string | null>(null);

    const isTourDismissed = useCallback((tourId: string) => dismissedTours.has(tourId), [dismissedTours]);

    const openTour = useCallback((tourId: string) => setActiveTourId(tourId), []);

    const dismissTour = useCallback((tourId: string) => {
        setDismissedTours(prev => {
            if (prev.has(tourId)) {
                return prev;
            }
            const next = new Set(prev);
            next.add(tourId);
            saveOnboardingState({ dismissedTours: Array.from(next) });
            return next;
        });
        setActiveTourId(current => (current === tourId ? null : current));
    }, []);

    const value = useMemo<OnboardingContextValue>(
        () => ({ activeTourId, isTourDismissed, openTour, dismissTour }),
        [activeTourId, isTourDismissed, openTour, dismissTour],
    );

    return <OnboardingContext.Provider value={value}>{children}</OnboardingContext.Provider>;
}

export function useOnboarding(): OnboardingContextValue {
    const ctx = useContext(OnboardingContext);
    if (!ctx) {
        throw new Error('useOnboarding must be used within an OnboardingProvider');
    }
    return ctx;
}

interface OnboardingTourHostProps {
    onNavigate: (navKey: RouteKey) => void;
}

export function OnboardingTourHost({ onNavigate }: Readonly<OnboardingTourHostProps>) {
    const { activeTourId, openTour, dismissTour, isTourDismissed } = useOnboarding();
    const autoStartAttempted = useRef(false);

    useEffect(() => {
        if (autoStartAttempted.current) {
            return;
        }
        autoStartAttempted.current = true;
        if (!isTourDismissed(APIM_OVERVIEW_TOUR_ID) && activeTourId === null) {
            openTour(APIM_OVERVIEW_TOUR_ID);
        }
    }, [activeTourId, isTourDismissed, openTour]);

    const activeTour = activeTourId ? TOURS[activeTourId] : undefined;

    return (
        <CoachmarkTour
            open={Boolean(activeTour)}
            steps={activeTour?.steps ?? []}
            onStepShown={step => step.navKey && onNavigate(step.navKey)}
            onComplete={() => activeTour && dismissTour(activeTour.id)}
            onSkip={() => activeTour && dismissTour(activeTour.id)}
        />
    );
}
