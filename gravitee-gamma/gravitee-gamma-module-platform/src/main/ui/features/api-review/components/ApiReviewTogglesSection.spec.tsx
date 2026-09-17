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
import { TooltipProvider } from '@gravitee/graphene-core';
import { fireEvent, render, screen } from '@testing-library/react';

import { ApiReviewTogglesSection } from './ApiReviewTogglesSection';

const NOT_READONLY = { apiScoreEnabled: false, apiReviewEnabled: false };

function renderSection(overrides: Partial<Parameters<typeof ApiReviewTogglesSection>[0]> = {}) {
    const onChange = jest.fn();
    render(
        <TooltipProvider>
            <ApiReviewTogglesSection
                value={{ apiScoreEnabled: true, apiReviewEnabled: false }}
                disabled={false}
                readonly={NOT_READONLY}
                onChange={onChange}
                {...overrides}
            />
        </TooltipProvider>,
    );
    return { onChange };
}

describe('ApiReviewTogglesSection', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('renders both toggles from the value', () => {
        renderSection();
        expect(screen.getByLabelText('Enable API Score')).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByLabelText('Enable API Review')).toHaveAttribute('aria-checked', 'false');
    });

    it('emits the next state when a toggle is flipped', () => {
        const { onChange } = renderSection();
        fireEvent.click(screen.getByLabelText('Enable API Review'));
        expect(onChange).toHaveBeenCalledWith({ apiScoreEnabled: true, apiReviewEnabled: true });
    });

    it('disables both toggles when the user cannot edit', () => {
        renderSection({ disabled: true });
        expect(screen.getByLabelText('Enable API Score')).toBeDisabled();
        expect(screen.getByLabelText('Enable API Review')).toBeDisabled();
    });

    it('locks a toggle the system pins and never emits for it', () => {
        const { onChange } = renderSection({ readonly: { apiScoreEnabled: false, apiReviewEnabled: true } });
        const reviewToggle = screen.getByLabelText('Enable API Review');
        expect(reviewToggle).toBeDisabled();
        expect(reviewToggle.closest('[data-system-readonly="true"]')).not.toBeNull();
        fireEvent.click(reviewToggle);
        expect(onChange).not.toHaveBeenCalled();
    });
});
