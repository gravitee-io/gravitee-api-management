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

import { renderHook } from '@testing-library/react';

import { useResolvedAuditDateRange } from './useResolvedAuditDateRange';
import type { AuditDatePreset } from '../types/auditLog';

const ANCHOR = 1_800_000_000_000;

describe('useResolvedAuditDateRange', () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('resolves a relative preset from the anchor, never from the wall clock', () => {
        // Any read of Date.now() inside the hook would let the window drift between renders.
        const nowSpy = jest.spyOn(Date, 'now').mockImplementation(() => {
            throw new Error('Date.now() must not be read while resolving the range');
        });

        const { result } = renderHook(() => useResolvedAuditDateRange('24h', undefined, ANCHOR));

        expect(result.current).toEqual({ from: ANCHOR - 24 * 60 * 60 * 1000, to: ANCHOR });
        expect(nowSpy).not.toHaveBeenCalled();
    });

    it('keeps relative from/to stable across rerenders so search keys do not churn', () => {
        const { result, rerender } = renderHook(
            ({ preset }: { preset: AuditDatePreset }) => useResolvedAuditDateRange(preset, undefined, ANCHOR),
            { initialProps: { preset: '24h' as AuditDatePreset } },
        );

        const first = result.current;
        rerender({ preset: '24h' });
        rerender({ preset: '24h' });

        expect(result.current).toEqual(first);
        expect(first.to).toBe(first.from! + 24 * 60 * 60 * 1000);
    });

    it('recomputes when the preset changes', () => {
        const { result, rerender } = renderHook(
            ({ preset }: { preset: AuditDatePreset }) => useResolvedAuditDateRange(preset, undefined, ANCHOR),
            { initialProps: { preset: '24h' as AuditDatePreset } },
        );

        const last24h = result.current;
        rerender({ preset: '7d' });

        expect(result.current).not.toEqual(last24h);
        expect(result.current.to! - result.current.from!).toBe(7 * 24 * 60 * 60 * 1000);
    });

    it('moves the window when the anchor is refreshed', () => {
        const { result, rerender } = renderHook(({ anchor }: { anchor: number }) => useResolvedAuditDateRange('24h', undefined, anchor), {
            initialProps: { anchor: ANCHOR },
        });

        rerender({ anchor: ANCHOR + 60_000 });

        expect(result.current).toEqual({ from: ANCHOR + 60_000 - 24 * 60 * 60 * 1000, to: ANCHOR + 60_000 });
    });

    it('keeps custom picker bounds stable when the range object identity changes', () => {
        const from = new Date('2026-08-01T08:00:00');
        const to = new Date('2026-08-03T08:00:00');
        const { result, rerender } = renderHook(
            ({ range }: { range: { from: Date; to: Date } }) => useResolvedAuditDateRange('custom', range, ANCHOR),
            { initialProps: { range: { from, to } } },
        );

        const first = result.current;
        rerender({ range: { from: new Date(from.getTime()), to: new Date(to.getTime()) } });
        expect(result.current).toEqual(first);
        expect(first.from).toBe(from.getTime());
    });
});
