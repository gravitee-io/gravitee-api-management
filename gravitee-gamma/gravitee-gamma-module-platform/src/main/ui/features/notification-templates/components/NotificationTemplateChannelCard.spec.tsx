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

import { NotificationTemplateChannelCard } from './NotificationTemplateChannelCard';
import type { NotificationTemplateDraft } from '../types/notificationTemplate';

jest.mock('@gravitee/graphene-core/code-editor', () => ({
    CodeEditor: ({ value, onChange, disabled }: { value?: string; onChange?: (next: string) => void; disabled?: boolean }) => (
        <textarea value={value} disabled={disabled} onChange={event => onChange?.(event.target.value)} />
    ),
}));

const BASE: NotificationTemplateDraft = { enabled: false, title: 'API started', content: '<p>started</p>' };

function Harness({
    initial = BASE,
    isInclude = false,
    disabled = false,
    showErrors = false,
}: {
    initial?: NotificationTemplateDraft;
    isInclude?: boolean;
    disabled?: boolean;
    showErrors?: boolean;
}) {
    const [draft, setDraft] = useState(initial);
    return (
        <NotificationTemplateChannelCard
            type="EMAIL"
            draft={draft}
            isInclude={isInclude}
            disabled={disabled}
            showErrors={showErrors}
            onChange={setDraft}
        />
    );
}

function contentGroup(): HTMLElement {
    return screen.getByRole('group', { name: 'Content' });
}

function contentEditor(): HTMLTextAreaElement {
    return contentGroup().querySelector('textarea') as HTMLTextAreaElement;
}

describe('NotificationTemplateChannelCard', () => {
    it('keeps title and content disabled until the override is turned on', () => {
        render(<Harness />);
        expect((screen.getByLabelText('Title of the notification') as HTMLInputElement).disabled).toBe(true);
        expect(contentEditor().disabled).toBe(true);
        fireEvent.click(screen.getByLabelText('Override default template'));
        expect((screen.getByLabelText('Title of the notification') as HTMLInputElement).disabled).toBe(false);
        expect(contentEditor().disabled).toBe(false);
        expect(screen.getByText('Custom')).not.toBeNull();
    });

    it('hides the title field for include fragments', () => {
        render(<Harness isInclude />);
        expect(screen.queryByLabelText('Title of the notification')).toBeNull();
        expect(contentEditor()).not.toBeNull();
    });

    it('shows required-field errors after a failed save attempt', () => {
        render(<Harness initial={{ enabled: true, title: '', content: '' }} showErrors />);
        const title = screen.getByLabelText('Title of the notification');
        const titleError = screen.getByText('Title of the notification is required.');
        const contentError = screen.getByText('Content is required.');
        expect(title.getAttribute('aria-invalid')).toBe('true');
        expect(title.getAttribute('aria-describedby')).toBe(titleError.id);
        expect(contentGroup().getAttribute('aria-invalid')).toBe('true');
        expect(contentGroup().getAttribute('aria-describedby')).toBe(contentError.id);
    });

    it('disables the override switch when the channel is read-only', () => {
        render(<Harness disabled />);
        expect((screen.getByLabelText('Override default template') as HTMLButtonElement).disabled).toBe(true);
    });
});
