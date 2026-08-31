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
import { Card, CardContent } from '@gravitee/graphene-core';

import { CategoryGrid } from './CategoryGrid';
import { FeaturedAgents } from './FeaturedAgents';
import { HeroSearch } from './HeroSearch';
import { useFeaturedAgents } from './useFeaturedAgents';
import { useCategories } from '../layout/useCategories';

export function HomePage() {
    const { agents, loading, error } = useFeaturedAgents();
    const categories = useCategories();

    return (
        <div className="space-y-8">
            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h1 className="text-2xl font-semibold">Discover agents</h1>
                    <HeroSearch />
                </CardContent>
            </Card>
            <FeaturedAgents agents={agents} loading={loading} error={error} />
            <CategoryGrid categories={categories} />
        </div>
    );
}
