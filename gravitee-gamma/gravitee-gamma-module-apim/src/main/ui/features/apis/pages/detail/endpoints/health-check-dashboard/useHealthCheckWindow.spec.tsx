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
import { act, renderHook } from '@testing-library/react';

import { useHealthCheckWindow } from './useHealthCheckWindow';

describe('useHealthCheckWindow', () => {
    afterEach(() => {
        jest.useRealTimers();
    });

    it('keeps one window until refresh, then moves the end forward', () => {
        jest.useFakeTimers();
        jest.setSystemTime(new Date('2026-01-01T00:00:00Z'));

        const { result } = renderHook(() => useHealthCheckWindow('1m'));
        const first = result.current.range;

        act(() => {
            jest.setSystemTime(new Date('2026-01-01T00:01:00Z'));
        });
        expect(result.current.range).toBe(first);

        act(() => result.current.refresh());
        expect(result.current.range.to).toBe(first.to + 60_000);
        expect(result.current.range.from).toBe(result.current.range.to - 60_000);
    });
});
