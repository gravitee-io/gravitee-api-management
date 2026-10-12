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

import { DiffDialog } from './DiffDialog';
import { SingleEventDialog } from './SingleEventDialog';
import type { ApiEvent } from '../../../types';

function event(id: string, version: string, definition: object): ApiEvent {
    return {
        id,
        createdAt: '2026-10-01T10:00:00Z',
        payload: JSON.stringify({ definition: JSON.stringify(definition) }),
        initiator: { id: 'u1', displayName: 'admin' },
        properties: { DEPLOYMENT_NUMBER: version },
    };
}

const LIVE = event('evt-live', '3', { name: 'Same name' });
const OLD = event('evt-old', '2', { name: 'Same name' });

describe('DiffDialog rollback', () => {
    it('offers a rollback for both compared versions', () => {
        render(
            <DiffDialog
                left={LIVE}
                right={OLD}
                canRollbackLeft
                canRollbackRight
                onClose={jest.fn()}
                onRollback={jest.fn()}
                isRollingBack={false}
            />,
        );

        expect(screen.getByRole('button', { name: 'Rollback to v3' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Rollback to v2' })).toBeInTheDocument();
    });

    it('hides the rollback of a version that cannot be rolled back to', () => {
        render(
            <DiffDialog
                left={LIVE}
                right={OLD}
                canRollbackLeft={false}
                canRollbackRight
                onClose={jest.fn()}
                onRollback={jest.fn()}
                isRollingBack={false}
            />,
        );

        expect(screen.queryByRole('button', { name: 'Rollback to v3' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Rollback to v2' })).toBeInTheDocument();
    });

    it('rolls back to the version whose button was used, even when both definitions are identical', () => {
        const onRollback = jest.fn().mockResolvedValue(undefined);
        render(
            <DiffDialog
                left={LIVE}
                right={OLD}
                canRollbackLeft
                canRollbackRight
                onClose={jest.fn()}
                onRollback={onRollback}
                isRollingBack={false}
            />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Rollback to v3' }));
        fireEvent.click(screen.getByRole('button', { name: /confirm rollback/i }));

        expect(onRollback).toHaveBeenCalledWith('evt-live');
    });
});

describe('SingleEventDialog rollback', () => {
    it('asks for confirmation before rolling back', () => {
        const onRollback = jest.fn().mockResolvedValue(undefined);
        render(<SingleEventDialog event={OLD} canRollback onRollback={onRollback} onClose={jest.fn()} isRollingBack={false} />);

        fireEvent.click(screen.getByRole('button', { name: 'Rollback to v2' }));

        expect(onRollback).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', { name: /confirm rollback/i }));
        expect(onRollback).toHaveBeenCalledWith('evt-old');
    });

    it('offers a rollback when the version can be rolled back to', () => {
        render(<SingleEventDialog event={OLD} canRollback onRollback={jest.fn()} onClose={jest.fn()} isRollingBack={false} />);

        expect(screen.getByRole('button', { name: 'Rollback to v2' })).toBeInTheDocument();
    });

    it('does not offer a rollback to the version already in use', () => {
        render(<SingleEventDialog event={LIVE} canRollback={false} onRollback={jest.fn()} onClose={jest.fn()} isRollingBack={false} />);

        expect(screen.queryByRole('button', { name: /rollback/i })).not.toBeInTheDocument();
    });
});
