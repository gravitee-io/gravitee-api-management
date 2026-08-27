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

import { TcpHostsCard } from './TcpHostsCard';
import type { TcpHostEntry } from '../../../types/apiCreation';

function rows(...hosts: string[]): TcpHostEntry[] {
    return hosts.map((host, i) => ({ id: `row-${i}`, host }));
}

const noop = () => {};

describe('TcpHostsCard row validation', () => {
    it('shows no error for a single valid host', () => {
        render(<TcpHostsCard rows={rows('db.example.com')} onAdd={noop} onDelete={noop} onHostChange={noop} isReadOnly={false} />);
        expect(screen.queryByText(/Duplicated hosts not allowed/)).not.toBeInTheDocument();
        expect(screen.queryByText(/Host is not valid/)).not.toBeInTheDocument();
    });

    it('shows a format error for an invalid hostname', () => {
        render(<TcpHostsCard rows={rows('not a valid host!')} onAdd={noop} onDelete={noop} onHostChange={noop} isReadOnly={false} />);
        expect(screen.getByText('Host is not valid')).toBeInTheDocument();
    });

    it('shows a duplicate error on every row sharing the same host', () => {
        render(
            <TcpHostsCard
                rows={rows('db.example.com', 'db.example.com')}
                onAdd={noop}
                onDelete={noop}
                onHostChange={noop}
                isReadOnly={false}
            />,
        );
        expect(screen.getAllByText('Duplicated hosts not allowed')).toHaveLength(2);
    });

    it('treats hosts as duplicate after trimming surrounding whitespace', () => {
        render(
            <TcpHostsCard
                rows={rows('db.example.com', ' db.example.com ')}
                onAdd={noop}
                onDelete={noop}
                onHostChange={noop}
                isReadOnly={false}
            />,
        );
        expect(screen.getAllByText('Duplicated hosts not allowed')).toHaveLength(2);
    });

    it('does not flag distinct hosts as duplicates', () => {
        render(
            <TcpHostsCard
                rows={rows('db.example.com', 'cache.example.com')}
                onAdd={noop}
                onDelete={noop}
                onHostChange={noop}
                isReadOnly={false}
            />,
        );
        expect(screen.queryByText(/Duplicated hosts not allowed/)).not.toBeInTheDocument();
    });

    it('shows a required error for a blank host row', () => {
        render(<TcpHostsCard rows={rows('')} onAdd={noop} onDelete={noop} onHostChange={noop} isReadOnly={false} />);
        expect(screen.getByText('Host is required.')).toBeInTheDocument();
    });
});
