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
import { toast } from '@gravitee/graphene-core';

import { notify } from './notify';
import { ApimApiError } from '../api/apimClient';

jest.mock('@gravitee/graphene-core', () => ({
    toast: {
        success: jest.fn(),
        error: jest.fn(),
        warning: jest.fn(),
        info: jest.fn(),
    },
}));

const mockToast = jest.mocked(toast);

describe('notify', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('shows success toasts with a transient duration', () => {
        notify.success('API saved');

        expect(mockToast.success).toHaveBeenCalledWith('API saved', { duration: 3000 });
    });

    it('shows info toasts with a transient duration', () => {
        notify.info('Copied to clipboard');

        expect(mockToast.info).toHaveBeenCalledWith('Copied to clipboard', { duration: 3000 });
    });

    it('shows warning toasts without overriding duration', () => {
        notify.warning('Plan published, but deployment is pending.');

        expect(mockToast.warning).toHaveBeenCalledWith('Plan published, but deployment is pending.');
    });

    it('shows error toasts with extracted Error messages and a long transient duration', () => {
        notify.error(new Error('Failed to save changes.'));

        expect(mockToast.error).toHaveBeenCalledWith('Failed to save changes.', { duration: 10_000 });
    });

    it('shows error toasts with ApimApiError messages', () => {
        notify.error(new ApimApiError(400, 'Bad request'));

        expect(mockToast.error).toHaveBeenCalledWith('Bad request', { duration: 10_000 });
    });

    it('uses the default fallback when the error cannot be extracted', () => {
        notify.error(null);

        expect(mockToast.error).toHaveBeenCalledWith('Something went wrong.', { duration: 10_000 });
    });

    it('uses a custom fallback when provided', () => {
        notify.error(null, 'Unable to delete plan');

        expect(mockToast.error).toHaveBeenCalledWith('Unable to delete plan', { duration: 10_000 });
    });
});
