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

import { ApplicationSharedApiKeyRenewDialog } from './ApplicationSharedApiKeyRenewDialog';

function renderDialog(canUseCustomApiKey = false) {
    const onConfirm = jest.fn();
    const onClose = jest.fn();
    render(
        <ApplicationSharedApiKeyRenewDialog
            open
            canUseCustomApiKey={canUseCustomApiKey}
            pending={false}
            onClose={onClose}
            onConfirm={onConfirm}
        />,
    );
    return { onConfirm, onClose };
}

describe('ApplicationSharedApiKeyRenewDialog', () => {
    it('explains the two-hour overlap and renews without a value when custom keys are disabled', () => {
        const { onConfirm } = renderDialog(false);

        expect(screen.getByTestId('shared-api-key-renew-notice')).toHaveTextContent(
            'The previous key stays valid for two hours. You can revoke it from the list.',
        );
        expect(screen.queryByTestId('shared-api-key-renew-value')).toBeNull();

        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));
        expect(onConfirm).toHaveBeenCalledWith(undefined);
    });

    it('rejects a short value and a forbidden character before submit', () => {
        const { onConfirm } = renderDialog(true);
        const input = screen.getByTestId('shared-api-key-renew-value');

        fireEvent.change(input, { target: { value: 'abc' } });
        expect(screen.getByTestId('shared-api-key-renew-error')).toHaveTextContent('Should have length between 8 and 128 characters');
        expect(screen.getByTestId('shared-api-key-renew-submit')).toBeDisabled();

        fireEvent.change(input, { target: { value: 'bad#key1' } });
        expect(screen.getByTestId('shared-api-key-renew-error')).toHaveTextContent('Should not contain');
        expect(screen.getByTestId('shared-api-key-renew-submit')).toBeDisabled();
        expect(onConfirm).not.toHaveBeenCalled();
    });

    it('submits a trimmed custom value', () => {
        const { onConfirm } = renderDialog(true);

        fireEvent.change(screen.getByTestId('shared-api-key-renew-value'), { target: { value: '  custom-key-1  ' } });
        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));

        expect(onConfirm).toHaveBeenCalledWith('custom-key-1');
    });

    it('cancel closes the dialog without renewing', () => {
        const { onConfirm, onClose } = renderDialog(true);

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalled();
        expect(onConfirm).not.toHaveBeenCalled();
    });
});
