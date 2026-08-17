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

import { BrandedSendersSection } from './BrandedSendersSection';
import type { BrandedSender } from '../types/consoleSettings';

const SENDERS: BrandedSender[] = [{ domains: ['partners.example.com'], from: 'Partners <partners@example.com>', subject: '[Partners] %s' }];

describe('BrandedSendersSection', () => {
    it('previews default from/subject and lists branded rules', () => {
        render(
            <BrandedSendersSection
                defaultFrom="noreply@example.com"
                defaultSubject="[gravitee] %s"
                senders={SENDERS}
                disabled={false}
                onChange={jest.fn()}
            />,
        );
        expect((screen.getByLabelText('Default From') as HTMLInputElement).value).toBe('noreply@example.com');
        expect((screen.getByLabelText('Default From') as HTMLInputElement).readOnly).toBe(true);
        expect(screen.getByText('partners.example.com')).not.toBeNull();
        expect((screen.getByLabelText('From *') as HTMLInputElement).value).toBe('Partners <partners@example.com>');
    });

    it('adds and removes branded sender rules', () => {
        const onChange = jest.fn();
        render(
            <BrandedSendersSection
                defaultFrom="noreply@example.com"
                defaultSubject="[gravitee] %s"
                senders={SENDERS}
                disabled={false}
                onChange={onChange}
            />,
        );
        fireEvent.click(screen.getByRole('button', { name: /Add rule/i }));
        expect(onChange).toHaveBeenCalledWith([...SENDERS, { domains: [], from: '', subject: '' }]);
        fireEvent.click(screen.getByRole('button', { name: /Delete branded sender 1/i }));
        expect(onChange).toHaveBeenCalledWith([]);
    });
});
