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
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, onClick, disabled }: { children?: ReactNode; onClick?: () => void; disabled?: boolean }) => (
        <button type="button" onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Dialog: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogClose: ({ children }: { children?: ReactNode }) => <>{children}</>,
    DialogContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    DialogFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
}));
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

import { SingleEventDialog } from './SingleEventDialog';
import type { ApiEvent } from '../../../types';

const event: ApiEvent = {
    id: 'evt-1',
    createdAt: '2026-01-01T00:00:00Z',
    payload: '{}',
    initiator: { id: 'u', displayName: 'Admin' },
    properties: { DEPLOYMENT_NUMBER: '1' },
};

describe('SingleEventDialog', () => {
    it('asks for confirmation before rolling back', () => {
        const onRollback = jest.fn();
        render(<SingleEventDialog event={event} canRollback onRollback={onRollback} onClose={() => {}} isRollingBack={false} />);
        fireEvent.click(screen.getByRole('button', { name: /rollback to v1/i }));
        expect(onRollback).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', { name: /confirm rollback/i }));
        expect(onRollback).toHaveBeenCalledWith('evt-1');
    });
});
