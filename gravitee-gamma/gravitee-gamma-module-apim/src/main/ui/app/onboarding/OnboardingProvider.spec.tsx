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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StrictMode } from 'react';

import { OnboardingProvider, OnboardingTourHost, useOnboarding } from './OnboardingProvider';
import { APIM_OVERVIEW_TOUR } from './tours';

const STORAGE_KEY = 'gravitee-apim-onboarding';

function Probe() {
    const { activeTourId, isTourDismissed, openTour, dismissTour } = useOnboarding();
    return (
        <div>
            <span data-testid="active">{activeTourId ?? 'none'}</span>
            <span data-testid="dismissed">{String(isTourDismissed('demo'))}</span>
            <button onClick={() => openTour('demo')}>open</button>
            <button onClick={() => dismissTour('demo')}>dismiss</button>
        </div>
    );
}

function renderProbe() {
    return render(
        <OnboardingProvider>
            <Probe />
        </OnboardingProvider>,
    );
}

describe('OnboardingProvider', () => {
    beforeEach(() => localStorage.clear());

    it('opens and tracks the active tour', async () => {
        const user = userEvent.setup();
        renderProbe();

        expect(screen.getByTestId('active')).toHaveTextContent('none');

        await user.click(screen.getByRole('button', { name: 'open' }));
        expect(screen.getByTestId('active')).toHaveTextContent('demo');
    });

    it('dismissing a tour persists it and closes the active tour', async () => {
        const user = userEvent.setup();
        renderProbe();

        await user.click(screen.getByRole('button', { name: 'open' }));
        await user.click(screen.getByRole('button', { name: 'dismiss' }));

        expect(screen.getByTestId('active')).toHaveTextContent('none');
        expect(screen.getByTestId('dismissed')).toHaveTextContent('true');
        expect(JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}').dismissedTours).toContain('demo');
    });

    it('reads dismissed tours from storage on mount', () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ dismissedTours: ['demo'] }));
        renderProbe();
        expect(screen.getByTestId('dismissed')).toHaveTextContent('true');
    });

    it('ignores malformed JSON in storage and treats nothing as dismissed', () => {
        localStorage.setItem(STORAGE_KEY, '{not valid json');
        renderProbe();
        expect(screen.getByTestId('dismissed')).toHaveTextContent('false');
    });

    it('ignores a wrongly-shaped persisted state', () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ dismissedTours: [42] }));
        renderProbe();
        expect(screen.getByTestId('dismissed')).toHaveTextContent('false');
    });

    it('throws when used outside a provider', () => {
        const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
        expect(() => render(<Probe />)).toThrow(/OnboardingProvider/);
        spy.mockRestore();
    });
});

describe('OnboardingTourHost', () => {
    beforeEach(() => localStorage.clear());

    const firstStep = APIM_OVERVIEW_TOUR.steps[0];

    function renderHost() {
        const onNavigate = jest.fn();
        render(
            <OnboardingProvider>
                <OnboardingTourHost onNavigate={onNavigate} />
            </OnboardingProvider>,
        );
        return { onNavigate };
    }

    it('auto-starts the overview tour on first landing and navigates to its first section', async () => {
        const { onNavigate } = renderHost();

        expect(await screen.findByText(firstStep.title)).toBeInTheDocument();
        await waitFor(() => expect(onNavigate).toHaveBeenCalledWith(firstStep.navKey));
    });

    it('does not auto-start once the tour has been dismissed', () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ dismissedTours: [APIM_OVERVIEW_TOUR.id] }));
        renderHost();
        expect(screen.queryByText(firstStep.title)).not.toBeInTheDocument();
    });

    it('auto-starts under StrictMode (real bootstrap wraps the tree in StrictMode)', async () => {
        render(
            <StrictMode>
                <OnboardingProvider>
                    <OnboardingTourHost onNavigate={jest.fn()} />
                </OnboardingProvider>
            </StrictMode>,
        );
        expect(await screen.findByText(firstStep.title)).toBeInTheDocument();
    });
});
