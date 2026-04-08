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
import { Button } from '@gravitee/graphene';
import { Link } from 'react-router-dom';

import type { GammaModule } from '../features/modules';

export function HomePage({
    modules,
    loading,
    error,
}: {
    readonly modules: GammaModule[];
    readonly loading: boolean;
    readonly error: Error | null;
}) {
    return (
        <>
            <h1>Welcome to Gravitee Gamma</h1>
            <h2>Shell</h2>
            <ul>
                <li>
                    <Button variant="link" asChild>
                        <Link to="/about">About</Link>
                    </Button>
                </li>
            </ul>
            <h2>Modules</h2>
            {loading && <p>Loading modules…</p>}
            {error && <p>Error loading modules: {error.message}</p>}
            {!loading && !error && modules.length === 0 && <p>No modules available.</p>}
            {modules.length > 0 && (
                <ul>
                    {modules.map(m => (
                        <li key={m.id}>
                            <Button variant="link" asChild>
                                <Link to={`/${m.id}`}>
                                    <strong>{m.name}</strong>
                                </Link>
                            </Button>{' '}
                            (v{m.version})
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
