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
import { fireEvent, render, screen } from '@testing-library/react';

jest.mock('../../hooks/useGatewayPrefix', () => ({
    useGatewayPrefix: () => 'https://gateway.example.com',
}));

const mockUseApiReviewEnabled = jest.fn(() => ({ enabled: false, isFetched: true }));
jest.mock('../../hooks/useApiReviewEnabled', () => ({
    useApiReviewEnabled: () => mockUseApiReviewEnabled(),
}));

import { ReviewDeployStep } from './ReviewDeployStep';
import { ApiCreationProvider, useApiCreation } from '../../store/apiCreationStore';

function CurrentOutcome() {
    const { state } = useApiCreation();
    return (
        <span data-testid="outcome">
            {String(state.form.askForReview)}/{String(state.form.deployImmediately)}
        </span>
    );
}

function renderStep() {
    return render(
        <ApiCreationProvider initialMode="scratch">
            <ReviewDeployStep />
            <CurrentOutcome />
        </ApiCreationProvider>,
    );
}

describe('ReviewDeployStep — creation outcome', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    afterEach(() => jest.clearAllMocks());

    it('offers the deploy switch while API Review is off', () => {
        mockUseApiReviewEnabled.mockReturnValue({ enabled: false, isFetched: true });
        renderStep();

        expect(screen.getByText('Deploy and start API immediately')).toBeInTheDocument();
        expect(screen.queryByLabelText('Ask for a review')).not.toBeInTheDocument();
        fireEvent.click(screen.getByLabelText('Deploy immediately'));
        expect(screen.getByTestId('outcome')).toHaveTextContent('true/false');
    });

    it('replaces the deploy switch with Ask for a review, on by default, while API Review is on', () => {
        mockUseApiReviewEnabled.mockReturnValue({ enabled: true, isFetched: true });
        renderStep();

        expect(screen.queryByLabelText('Deploy immediately')).not.toBeInTheDocument();
        expect(screen.getByText(/a reviewer must accept it before it can be started/i)).toBeInTheDocument();
        const askSwitch = screen.getByLabelText('Ask for a review');
        expect(askSwitch).toHaveAttribute('aria-checked', 'true');

        fireEvent.click(askSwitch);
        expect(screen.getByTestId('outcome')).toHaveTextContent('false/true');
    });
});
