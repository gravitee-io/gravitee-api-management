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

import { GrantTypeChips } from './GrantTypeChips';
import { APPLICATION_TYPES_FIXTURE } from '../../fixtures/applicationTypes.fixture';

describe('GrantTypeChips', () => {
    const spaType = APPLICATION_TYPES_FIXTURE.find(type => type.id === 'browser')!;
    const webType = APPLICATION_TYPES_FIXTURE.find(type => type.id === 'web')!;

    it('toggles optional grant types for SPA', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(<GrantTypeChips typeConfig={spaType} values={['authorization_code']} onChange={onChange} />);

        await user.click(screen.getByRole('button', { name: /Implicit/i }));

        expect(onChange).toHaveBeenCalledWith(['authorization_code', 'implicit']);
    });

    it('does not deselect mandatory grant types for Web', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(<GrantTypeChips typeConfig={webType} values={['authorization_code', 'refresh_token']} onChange={onChange} />);

        await user.click(screen.getByRole('button', { name: /Authorization Code/i }));

        expect(onChange).not.toHaveBeenCalled();
    });

    it('allows selecting mandatory grant types when not yet selected', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(<GrantTypeChips typeConfig={webType} values={['refresh_token']} onChange={onChange} />);

        await user.click(screen.getByRole('button', { name: /Authorization Code/i }));

        expect(onChange).toHaveBeenCalledWith(['refresh_token', 'authorization_code']);
    });

    it('does not toggle when disabled', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(<GrantTypeChips typeConfig={spaType} values={['authorization_code']} onChange={onChange} disabled />);

        await user.click(screen.getByRole('button', { name: /Implicit/i }));

        expect(onChange).not.toHaveBeenCalled();
    });
});
