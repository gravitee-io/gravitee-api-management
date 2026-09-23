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

const mockMutate = jest.fn();
jest.mock('./useApiReviewMutations', () => ({
    useAskApiReview: () => ({ mutate: mockMutate, isPending: false }),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

import { useAskForReviewDialog } from './useAskForReviewDialog';
import { notify } from '../../../shared/notify';

describe('useAskForReviewDialog', () => {
    afterEach(() => jest.clearAllMocks());

    it('starts closed and opens on demand', () => {
        const { result } = renderHook(() => useAskForReviewDialog('api-1'));

        expect(result.current.dialogProps.open).toBe(false);
        act(() => result.current.openDialog());
        expect(result.current.dialogProps.open).toBe(true);
    });

    it('carries the same copy both call sites show', () => {
        const { result } = renderHook(() => useAskForReviewDialog('api-1'));

        expect(result.current.dialogProps.title).toBe('Review API');
        expect(result.current.dialogProps.description).toBe('Are you sure you want to ask for a review of the API?');
        expect(result.current.dialogProps.confirmLabel).toBe('Ask for review');
    });

    it('closes and reports success once the review is asked', () => {
        const { result } = renderHook(() => useAskForReviewDialog('api-1'));
        act(() => result.current.openDialog());

        act(() => result.current.dialogProps.onConfirm());
        expect(mockMutate).toHaveBeenCalledWith(undefined, expect.objectContaining({ onSuccess: expect.any(Function) }));

        const handlers = mockMutate.mock.calls[0]?.[1] as { onSuccess: () => void };
        act(() => handlers.onSuccess());
        expect(result.current.dialogProps.open).toBe(false);
        expect(notify.success).toHaveBeenCalledWith('Review has been asked.');
    });

    it('keeps the dialog open and reports the failure when asking fails', () => {
        const { result } = renderHook(() => useAskForReviewDialog('api-1'));
        act(() => result.current.openDialog());
        act(() => result.current.dialogProps.onConfirm());

        const handlers = mockMutate.mock.calls[0]?.[1] as { onError: (e: Error) => void };
        const failure = new Error('nope');
        act(() => handlers.onError(failure));

        expect(result.current.dialogProps.open).toBe(true);
        expect(notify.error).toHaveBeenCalledWith(failure, 'Review has not been asked.');
    });
});
