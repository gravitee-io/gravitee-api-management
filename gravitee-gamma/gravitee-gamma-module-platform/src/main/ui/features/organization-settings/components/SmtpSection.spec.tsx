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

import { TooltipProvider } from '@gravitee/graphene-core';
import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import { isSmtpFormValid, isSmtpFromValid, SmtpSection, type SmtpFieldReadonly, type SmtpFormState } from './SmtpSection';
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
    return (
        <TooltipProvider>
            <SmtpSection value={value} disabled={disabled} readonly={readonly} onChange={setValue} />
        </TooltipProvider>
    );
}

describe('SmtpSection', () => {
    it('wraps SMTP blocks in Graphene Cards', () => {
        render(<Harness />);
        expect(screen.getByLabelText('Enable Emailing').closest('[data-slot="card"]')).not.toBeNull();
        expect(screen.getByText('Mail Properties').closest('[data-slot="card-title"]')).not.toBeNull();
        expect(screen.getByText('Branded notification email').closest('[data-slot="card"]')).not.toBeNull();
    });

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
        expect(isSmtpFormValid({ ...ENABLED, port: '0' })).toBe(true);
        expect(isSmtpFormValid({ ...ENABLED, port: '65536' })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, from: 'not-an-email' })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, from: 'Partners <partners@example.com>' })).toBe(true);
        expect(isSmtpFormValid({ ...ENABLED, enabled: false, host: '' })).toBe(true);
    });

    it('accepts single-label From addresses used with local SMTP (Classic EMAIL_PATTERN / backend SenderAddressValidator)', () => {
        expect(isSmtpFromValid('user@localhost')).toBe(true);
        expect(isSmtpFromValid('Team <user@localhost>')).toBe(true);
        expect(isSmtpFromValid('notifications@mailhog')).toBe(true);
        expect(isSmtpFromValid('noreply@example.com')).toBe(true);
        expect(isSmtpFromValid('not-an-email')).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, from: 'user@localhost' })).toBe(true);
    });

    it('requires every branded sender rule to have at least one domain and a valid from address', () => {
        const validRule = { domains: ['partners.example.com'], from: 'partners@example.com', subject: '' };
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [validRule] })).toBe(true);
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, domains: [] }] })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, from: '' }] })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, from: 'not-an-email' }] })).toBe(false);
        // Incomplete branded sender rules don't block save while emailing itself is disabled.
        expect(isSmtpFormValid({ ...ENABLED, enabled: false, brandedSenders: [{ domains: [], from: '', subject: '' }] })).toBe(true);
    });

    it('rejects branded domains that Classic / the backend would reject', () => {
        const validRule = { domains: ['partners.example.com'], from: 'partners@example.com', subject: '' };
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, domains: ['invalid domain'] }] })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, domains: ['localhost'] }] })).toBe(false);
        expect(
            isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, domains: ['partners.example.com', 'Partners.Example.com'] }] }),
        ).toBe(false);
        expect(
            isSmtpFormValid({
                ...ENABLED,
                brandedSenders: [
                    { domains: ['EXAMPLE.COM'], from: 'b@example.com', subject: '' },
                    { domains: ['example.com'], from: 'c@example.com', subject: '' },
                ],
            }),
        ).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, subject: 'x'.repeat(256) }] })).toBe(false);
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: [{ ...validRule, subject: '' }] })).toBe(true);
    });

    it('rejects branded sender lists that would overflow the 4000-character storage cap', () => {
        const oversized = {
            domains: [`${'a'.repeat(60)}.example.com`],
            from: 'partners@example.com',
            subject: 'x'.repeat(255),
        };
        const senders = Array.from({ length: 20 }, () => ({ ...oversized, domains: [...oversized.domains] }));
        expect(isSmtpFormValid({ ...ENABLED, brandedSenders: senders })).toBe(false);
    });

    it('shows Classic branded-sender field errors on the form', () => {
        render(
            <Harness
                initial={{
                    ...ENABLED,
                    brandedSenders: [{ domains: ['localhost'], from: '', subject: '' }],
                }}
            />,
        );
        expect(screen.getByText('Invalid domain(s): localhost')).not.toBeNull();
        expect(screen.getByText('From is required.')).not.toBeNull();
        expect(screen.getByLabelText('Recipient domains *').getAttribute('aria-invalid')).toBe('true');
        expect(screen.getByLabelText('From *').getAttribute('aria-invalid')).toBe('true');
    });

    it('marks system-provided SMTP fields for the Classic tooltip', () => {
        render(<Harness readonly={{ enabled: true, host: true }} />);
        expect(screen.getByLabelText('Enable Emailing').closest('[data-system-readonly="true"]')).not.toBeNull();
        expect(screen.getByLabelText('Host').closest('[data-system-readonly="true"]')).not.toBeNull();
        expect(screen.getByLabelText('Username').closest('[data-system-readonly="true"]')).toBeNull();
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
