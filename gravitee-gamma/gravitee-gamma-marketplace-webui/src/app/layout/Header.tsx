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
import { Button, InputGroup, InputGroupAddon, InputGroupInput, TopNavUser } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState, type KeyboardEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useLogout, useUser } from '../auth';

export function Header() {
    const user = useUser();
    const logout = useLogout();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const urlQuery = searchParams.get('query') ?? '';
    const [query, setQuery] = useState(urlQuery);

    useEffect(() => {
        setQuery(urlQuery);
    }, [urlQuery]);

    const submitSearch = () => {
        const trimmed = query.trim();
        navigate(trimmed ? `/catalog?query=${encodeURIComponent(trimmed)}` : '/catalog');
    };

    const handleSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            submitSearch();
        }
    };

    return (
        <>
            <div className="w-64" role="search">
                <label htmlFor="global-agent-search" className="sr-only">
                    Search agents
                </label>
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        id="global-agent-search"
                        name="query"
                        placeholder="Search agents..."
                        value={query}
                        onChange={event => setQuery(event.target.value)}
                        onKeyDown={handleSearchKeyDown}
                    />
                </InputGroup>
            </div>
            {user ? (
                <TopNavUser name={user.display_name || user.email || 'Account'} email={user.email} onSignOut={() => void logout()} />
            ) : (
                <Button type="button" variant="outline" size="sm" onClick={() => navigate('/login')}>
                    Sign in
                </Button>
            )}
        </>
    );
}
