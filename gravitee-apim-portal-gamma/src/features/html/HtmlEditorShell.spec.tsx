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

jest.mock('./hydrate-slots', () => ({
    HtmlSlotHydrator: () => null,
}));

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { HtmlEditorShell } from './HtmlEditorShell';

describe('HtmlEditorShell', () => {
    it('should render split layout with HTML, CSS, and preview labels', () => {
        renderWithGraphene(
            <HtmlEditorShell
                html="<h1>Title</h1>"
                css=".title { color: red; }"
                scopeId="page-test"
                layout="split"
                onHtmlChange={jest.fn()}
                onCssChange={jest.fn()}
            />,
        );

        expect(screen.getByText('HTML')).toBeInTheDocument();
        expect(screen.getByText('CSS')).toBeInTheDocument();
        expect(screen.getByText('Title')).toBeInTheDocument();
    });

    it('should render tabs layout and switch between editors', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <HtmlEditorShell
                html="<h1>Title</h1>"
                css=".title { color: red; }"
                scopeId="page-test"
                layout="tabs"
                onHtmlChange={jest.fn()}
                onCssChange={jest.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: 'Preview' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'HTML' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'CSS' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'HTML' }));
        expect(screen.getByLabelText('HTML source')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'CSS' }));
        expect(screen.getByLabelText('CSS')).toBeInTheDocument();
    });
});
