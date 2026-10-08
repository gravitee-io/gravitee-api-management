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
import { act, renderHook } from '@testing-library/react';

import { useMinimumDuration } from './useMinimumDuration';

const MINIMUM_MS = 600;

function renderMinimumDuration(active: boolean) {
    return renderHook((value: boolean) => useMinimumDuration(value, MINIMUM_MS), { initialProps: active });
}

describe('useMinimumDuration', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('should stay false while the value is false', () => {
        const { result } = renderMinimumDuration(false);

        act(() => jest.advanceTimersByTime(MINIMUM_MS));

        expect(result.current).toBe(false);
    });

    it('should keep a value that turned false right away true until the minimum has passed', () => {
        const { result, rerender } = renderMinimumDuration(false);

        rerender(true);
        expect(result.current).toBe(true);

        act(() => jest.advanceTimersByTime(25));
        rerender(false);
        act(() => jest.advanceTimersByTime(MINIMUM_MS - 25 - 1));
        expect(result.current).toBe(true);

        act(() => jest.advanceTimersByTime(1));
        expect(result.current).toBe(false);
    });

    it('should follow a value that stays true longer than the minimum', () => {
        const { result, rerender } = renderMinimumDuration(true);

        act(() => jest.advanceTimersByTime(MINIMUM_MS * 3));
        expect(result.current).toBe(true);

        rerender(false);
        expect(result.current).toBe(false);
    });

    it('should count the minimum again each time the value turns true', () => {
        const { result, rerender } = renderMinimumDuration(true);

        rerender(false);
        act(() => jest.advanceTimersByTime(MINIMUM_MS - 100));
        rerender(true);
        rerender(false);
        act(() => jest.advanceTimersByTime(MINIMUM_MS - 1));
        expect(result.current).toBe(true);

        act(() => jest.advanceTimersByTime(1));
        expect(result.current).toBe(false);
    });
});
