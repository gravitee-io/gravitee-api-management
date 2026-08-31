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
import { Button, Input } from '@gravitee/graphene-core';
import { useState, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';

export function HeroSearch() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');

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
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <label htmlFor="home-agent-search" className="sr-only">
                Find an agent
            </label>
            <Input
                id="home-agent-search"
                type="search"
                placeholder="Find an agent..."
                value={query}
                onChange={event => setQuery(event.target.value)}
                onKeyDown={handleSearchKeyDown}
                className="sm:flex-1"
            />
            <Button type="button" onClick={submitSearch}>
                Search
            </Button>
        </div>
    );
}
