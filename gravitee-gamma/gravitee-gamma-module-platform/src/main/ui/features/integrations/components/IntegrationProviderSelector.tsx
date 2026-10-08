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

import {
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    Item,
    ItemContent,
    ItemDescription,
    ItemGroup,
    ItemMedia,
    ItemTitle,
} from '@gravitee/graphene-core';
import { ChevronRightIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { ProviderMonogram } from './ProviderMonogram';
import { PROVIDER_CATALOG, PROVIDER_GROUPS_IN_ORDER, type ProviderCatalogEntry } from '../utils/providerLabels';

function matchesFilter(entry: ProviderCatalogEntry, filter: string): boolean {
    const needle = filter.trim().toLowerCase();
    return entry.label.toLowerCase().includes(needle) || entry.description.toLowerCase().includes(needle);
}

export function IntegrationProviderSelector({ onSelect }: Readonly<{ onSelect: (provider: ProviderCatalogEntry) => void }>) {
    const [filter, setFilter] = useState('');

    const matching = PROVIDER_CATALOG.filter(entry => matchesFilter(entry, filter));
    const groups = PROVIDER_GROUPS_IN_ORDER.map(group => ({ group, providers: matching.filter(entry => entry.group === group) })).filter(
        ({ providers }) => providers.length > 0,
    );

    return (
        <div className="space-y-6">
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        aria-label="Filter providers"
                        placeholder="Filter providers"
                        value={filter}
                        onChange={event => setFilter(event.target.value)}
                    />
                </InputGroup>
            </div>
            {groups.length === 0 ? <p className="text-sm text-muted-foreground">No providers match &quot;{filter.trim()}&quot;.</p> : null}
            {groups.map(({ group, providers }) => (
                <section key={group} aria-label={group} className="space-y-2">
                    <h2 className="text-xs font-medium text-muted-foreground">{group}</h2>
                    <ItemGroup className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        {providers.map(provider => (
                            <Item key={provider.token} variant="outline" asChild>
                                <button type="button" className="text-left" onClick={() => onSelect(provider)}>
                                    <ItemMedia>
                                        <ProviderMonogram monogram={provider.monogram} />
                                    </ItemMedia>
                                    <ItemContent>
                                        <ItemTitle>{provider.label}</ItemTitle>
                                        <ItemDescription>{provider.description}</ItemDescription>
                                    </ItemContent>
                                    <ChevronRightIcon className="size-4 text-muted-foreground" aria-hidden />
                                </button>
                            </Item>
                        ))}
                    </ItemGroup>
                </section>
            ))}
        </div>
    );
}
