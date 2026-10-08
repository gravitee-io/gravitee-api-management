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
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, disabled }: { children?: ReactNode; disabled?: boolean }) => (
        <button type="button" disabled={disabled}>
            {children}
        </button>
    ),
    Sheet: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div>{children}</div> : null),
    SheetContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    SheetFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
}));
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));
jest.mock('./SegmentedTabs', () => ({
    SegmentedTabs: () => <div />,
}));
jest.mock('./CheckboxOptionList', () => ({
    CheckboxOptionList: () => <div />,
}));

import { ExportApi } from './ExportApi';

describe('ExportApi', () => {
    it('keeps the Export button visible and labelled Exporting while an export is running', () => {
        render(<ExportApi open onOpenChange={() => {}} onExport={() => {}} isExporting />);
        expect(screen.getByRole('button', { name: /exporting/i })).toBeDisabled();
    });
});
