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
import { ThemeProvider } from '@gravitee/graphene-core';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';

import { notify } from '@apim/portal-editor/shared/notify/notify';
import { ImportFromClassicDialog } from './ImportFromClassicDialog';

jest.mock('@apim/portal-editor/shared/notify/notify', () => ({
    notify: {
        info: jest.fn(),
        success: jest.fn(),
        error: jest.fn(),
        warning: jest.fn(),
    },
}));

const mockNotifyInfo = jest.mocked(notify.info);

function renderDialog(ui: ReactElement) {
    return render(<ThemeProvider>{ui}</ThemeProvider>);
}

describe('ImportFromClassicDialog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('should show a scanning state then render the classic page tree', async () => {
        renderDialog(
            <ImportFromClassicDialog open apiName="Payments API" onOpenChange={jest.fn()} />,
        );

        expect(screen.getByTestId('classic-import-scanning')).toBeInTheDocument();
        expect(screen.getByText(/scanning classic console documentation for/i)).toBeInTheDocument();

        await act(async () => {
            jest.advanceTimersByTime(600);
        });

        await waitFor(() => {
            expect(screen.getByTestId('classic-page-tree')).toBeInTheDocument();
        });

        expect(screen.getByText(/overview \(markdown, homepage\)/i)).toBeInTheDocument();
        expect(screen.getByText(/authentication \(markdown\) — has attached media/i)).toBeInTheDocument();
        expect(screen.getByText(/legacy asciidoc page \(asciidoc\) — unsupported format/i)).toBeInTheDocument();
    });

    it('should show the limitations alert', async () => {
        renderDialog(
            <ImportFromClassicDialog open apiName="Payments API" onOpenChange={jest.fn()} />,
        );

        await act(async () => {
            jest.advanceTimersByTime(600);
        });

        await waitFor(() => {
            expect(screen.getByTestId('classic-import-limitations')).toBeInTheDocument();
        });

        expect(screen.getByText(/translations \(i18n pages\)/i)).toBeInTheDocument();
        expect(screen.getByText(/page revision history — not migrated/i)).toBeInTheDocument();
    });

    it('should show a preview-only toast and close when Import mock is clicked', async () => {
        const onOpenChange = jest.fn();

        renderDialog(
            <ImportFromClassicDialog open apiName="Payments API" onOpenChange={onOpenChange} />,
        );

        await act(async () => {
            jest.advanceTimersByTime(600);
        });

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /^import \(mock\)$/i })).toBeEnabled();
        });

        await act(async () => {
            screen.getByRole('button', { name: /^import \(mock\)$/i }).click();
        });

        expect(mockNotifyInfo).toHaveBeenCalledWith('Import preview only — no changes were made to the draft.');
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('should close without showing a toast when Cancel is clicked', async () => {
        jest.useRealTimers();
        const user = userEvent.setup();
        const onOpenChange = jest.fn();

        renderDialog(
            <ImportFromClassicDialog open apiName="Payments API" onOpenChange={onOpenChange} />,
        );

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /^cancel$/i })).toBeInTheDocument();
        });

        await user.click(screen.getByRole('button', { name: /^cancel$/i }));

        expect(mockNotifyInfo).not.toHaveBeenCalled();
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });
});
