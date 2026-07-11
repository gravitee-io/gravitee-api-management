/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
jest.mock('react-resizable-panels', () => ({
    Group: ({ children, className }: { readonly children?: React.ReactNode; readonly className?: string }) => (
        <div className={className}>{children}</div>
    ),
    Panel: ({ children, className }: { readonly children?: React.ReactNode; readonly className?: string }) => (
        <div className={className}>{children}</div>
    ),
    Separator: ({ className }: { readonly className?: string }) => <div className={className} role="separator" />,
}));

jest.mock('@gravitee/graphene-core/code-editor', () => ({
    CodeEditor: ({
        value,
        onChange,
        'aria-label': ariaLabel,
    }: {
        readonly value?: string;
        readonly onChange?: (value: string) => void;
        readonly 'aria-label'?: string;
    }) => (
        <textarea
            aria-label={ariaLabel ?? 'HTML source'}
            value={value}
            onChange={event => onChange?.(event.target.value)}
        />
    ),
}));

jest.mock('../../html/hydrate-slots', () => ({
    HtmlSlotHydrator: () => null,
}));

import { createRef } from 'react';
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { HtmlPageEditor, type HtmlPageEditorHandle } from './HtmlPageEditor';

describe('HtmlPageEditor', () => {
    const content = {
        id: 'content-html',
        portalId: 'portal-1',
        navigationItemId: 'page-html',
        contentType: 'HTML' as const,
        html: '<h1>Hello</h1>',
        css: '.page { padding: 1rem; }',
    };

    it('should render split layout controls by default', () => {
        renderWithGraphene(<HtmlPageEditor content={content} onSave={jest.fn().mockResolvedValue(undefined)} />);

        expect(screen.getByRole('group', { name: 'Editor layout' })).toBeInTheDocument();
        expect(screen.getByText('HTML')).toBeInTheDocument();
        expect(screen.getByText('CSS')).toBeInTheDocument();
        expect(screen.getByText('Hello')).toBeInTheDocument();
    });

    it('should persist html and css on save', async () => {
        const user = userEvent.setup();
        const onSave = jest.fn().mockResolvedValue(undefined);
        const ref = createRef<HtmlPageEditorHandle>();

        renderWithGraphene(<HtmlPageEditor ref={ref} content={content} onSave={onSave} />);

        await user.clear(screen.getByLabelText('CSS'));
        fireEvent.change(screen.getByLabelText('CSS'), {
            target: { value: '.updated { color: blue; }' },
        });

        await ref.current?.save();

        await waitFor(() => {
            expect(onSave).toHaveBeenCalledWith({
                ...content,
                css: '.updated { color: blue; }',
            });
        });
    });
});
