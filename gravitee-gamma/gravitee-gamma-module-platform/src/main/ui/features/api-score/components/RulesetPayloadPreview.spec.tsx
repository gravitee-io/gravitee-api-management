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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { RulesetPayloadPreview } from './RulesetPayloadPreview';

jest.mock('../../../shared/copyToClipboard', () => ({
    copyTextToClipboardWithNotifyHandler: jest.fn(),
}));

const { copyTextToClipboardWithNotifyHandler } = jest.requireMock('../../../shared/copyToClipboard') as {
    copyTextToClipboardWithNotifyHandler: jest.Mock;
};

describe('RulesetPayloadPreview', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders numbered lines inside a scrollable preview and copies the full payload', async () => {
        const user = userEvent.setup();
        const payload = 'rules:\n  - id: one';
        render(<RulesetPayloadPreview payload={payload} />);

        expect(screen.getByTestId('ruleset-payload-preview-scroll')).not.toBeNull();
        expect(screen.getByText('1')).not.toBeNull();
        expect(screen.getByText('2')).not.toBeNull();
        expect(screen.getByText('rules:')).not.toBeNull();
        expect(screen.getByText('- id: one')).not.toBeNull();

        await user.click(screen.getByRole('button', { name: /copy code to clipboard/i }));
        expect(copyTextToClipboardWithNotifyHandler).toHaveBeenCalledWith(payload, 'Copied to clipboard');
    });
});
