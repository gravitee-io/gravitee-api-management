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
import { Card, CardContent, CardHeader } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { useIsAuthenticated } from '../auth';

export function HomePage() {
    const isAuthenticated = useIsAuthenticated();

    return (
        <Card>
            <CardHeader>
                <h1>Hello World</h1>
            </CardHeader>
            <CardContent>
                <p>Marketplace portal</p>
                {isAuthenticated ? <Link to="/dashboard">Dashboard</Link> : <Link to="/login">Sign in</Link>}
            </CardContent>
        </Card>
    );
}
