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
import { RadioIcon, SparklesIcon } from '@gravitee/graphene-core/icons';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { CoachmarkTour } from './CoachmarkTour';
import type { CoachmarkStep } from './tours';

const STEPS: CoachmarkStep[] = [
    { id: 'one', icon: SparklesIcon, title: 'First step', description: 'Intro' },
    { id: 'two', icon: RadioIcon, title: 'Second step', description: 'Details' },
];

function setup(overrides: Partial<Parameters<typeof CoachmarkTour>[0]> = {}) {
    const onComplete = jest.fn();
    const onSkip = jest.fn();
    render(<CoachmarkTour open steps={STEPS} onComplete={onComplete} onSkip={onSkip} {...overrides} />);
    return { onComplete, onSkip, user: userEvent.setup() };
}

describe('CoachmarkTour', () => {
    it('shows the first step with progress and no Back button', () => {
        setup();
        expect(screen.getByText('Step 1 of 2')).toBeInTheDocument();
        expect(screen.getByText('First step')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Back' })).not.toBeInTheDocument();
    });

    it('advances through steps and completes on the last one', async () => {
        const { onComplete, user } = setup();

        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(screen.getByText('Step 2 of 2')).toBeInTheDocument();
        expect(screen.getByText('Second step')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Back' }));
        expect(screen.getByText('Step 1 of 2')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: 'Done' }));
        expect(onComplete).toHaveBeenCalledTimes(1);
    });

    it('skips the tour from any step', async () => {
        const { onSkip, user } = setup();
        await user.click(screen.getByRole('button', { name: 'Skip tour' }));
        expect(onSkip).toHaveBeenCalledTimes(1);
    });

    it('announces each shown step on open and on navigation', async () => {
        const onStepShown = jest.fn();
        const { user } = setup({ onStepShown });

        expect(onStepShown).toHaveBeenLastCalledWith(STEPS[0]);

        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(onStepShown).toHaveBeenLastCalledWith(STEPS[1]);

        await user.click(screen.getByRole('button', { name: 'Back' }));
        expect(onStepShown).toHaveBeenLastCalledWith(STEPS[0]);
    });

    it('renders nothing when there are no steps', () => {
        const { container } = render(<CoachmarkTour open steps={[]} onComplete={jest.fn()} onSkip={jest.fn()} />);
        expect(container).toBeEmptyDOMElement();
    });
});
