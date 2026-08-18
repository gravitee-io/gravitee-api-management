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
import { useState } from 'react';

import { isSmtpFormValid, SmtpSection, type SmtpFieldReadonly, type SmtpFormState } from './SmtpSection';
import { PASSWORD_SENTINEL } from '../types/consoleSettings';

const ENABLED: SmtpFormState = {
    enabled: true,
    host: 'smtp.example.com',
    port: '587',
    username: 'admin',
    password: PASSWORD_SENTINEL,
    protocol: 'smtp',
    subject: '[gravitee] %s',
    from: 'noreply@example.com',
    auth: true,
    startTlsEnable: true,
    sslTrust: '',
    brandedSenders: [],
};

function Harness({
    initial = ENABLED,
    disabled = false,
    readonly,
}: {
    initial?: SmtpFormState;
    disabled?: boolean;
    readonly?: SmtpFieldReadonly;
}) {
    const [value, setValue] = useState(initial);
    return <SmtpSection value={value} disabled={disabled} readonly={readonly} onChange={setValue} />;
}

describe('SmtpSection', () => {
    it('hides mail fields when emailing is disabled', () => {
        render(<Harness initial={{ ...ENABLED, enabled: false }} />);
        expect(screen.queryByLabelText('Host')).toBeNull();
        expect(screen.getByLabelText('Enable Emailing')).not.toBeNull();
    });

    it('keeps branded senders visible but disabled when emailing is off', () => {
        render(
            <Harness
                initial={{
                    ...ENABLED,
                    enabled: false,
                    brandedSenders: [
                        { domains: ['partners.example.com'], from: 'Partners <partners@example.com>', subject: '[Partners] %s' },
                    ],
                }}
            />,
        );
        expect(screen.getByText('Branded notification email')).not.toBeNull();
        expect(screen.queryByRole('button', { name: /Add rule/i })).toBeNull();
        expect((screen.getByLabelText('From *') as HTMLInputElement).disabled).toBe(true);
    });

    it('shows host, port, password sentinel, and mail properties when enabled', () => {
        render(<Harness />);
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.example.com');
        expect((screen.getByLabelText('Port') as HTMLInputElement).value).toBe('587');
        expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe(PASSWORD_SENTINEL);
        expect(screen.getByLabelText('Enable Auth')).not.toBeNull();
        expect(screen.getByLabelText('Enable Start TLS')).not.toBeNull();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.acme.com' } });
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.acme.com');
    });

    it('requires host, a valid port, and from when enabled', () => {
        expect(isSmtpFormValid({ ...ENABLED, host: '' })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, port: '0' })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, from: 'not-an-email' })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, from: 'Partners <partners@example.com>' })).toBe(true);
        expect(isSmtpFormValid({ ...ENABLED, enabled: false, host: '' })).toBe(true);
    });

    it('lets the user turn emailing on even when host is system-provided', () => {
        render(<Harness initial={{ ...ENABLED, enabled: false }} readonly={{ host: true }} />);
        const enableSwitch = screen.getByLabelText('Enable Emailing') as HTMLButtonElement;
        expect(enableSwitch.disabled).toBe(false);
        fireEvent.click(enableSwitch);
        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByLabelText('Username') as HTMLInputElement).disabled).toBe(false);
    });
});
