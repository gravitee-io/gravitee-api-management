/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { RetryButton } from './RetryButton';

describe('RetryButton', () => {
    afterEach(() => {
        jest.useRealTimers();
    });

    it('should retry when clicked', async () => {
        const user = userEvent.setup();
        const onRetry = jest.fn();
        render(<RetryButton retrying={false} onRetry={onRetry} />);

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('should stay in place, without acting, while a retry is running', async () => {
        const user = userEvent.setup();
        const onRetry = jest.fn();
        render(<RetryButton retrying onRetry={onRetry} />);

        const button = screen.getByRole('button', { name: 'Retrying…' });
        expect(button.getAttribute('aria-disabled')).toBe('true');
        await user.click(button);

        expect(onRetry).not.toHaveBeenCalled();
    });

    it('should keep "Retrying…" long enough to be read when the retry fails right away', () => {
        jest.useFakeTimers();
        const { rerender } = render(<RetryButton retrying onRetry={jest.fn()} />);

        act(() => jest.advanceTimersByTime(25));
        rerender(<RetryButton retrying={false} onRetry={jest.fn()} />);
        act(() => jest.advanceTimersByTime(574));
        expect(screen.getByRole('button', { name: 'Retrying…' })).toBeTruthy();

        act(() => jest.advanceTimersByTime(1));
        expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
    });
});
