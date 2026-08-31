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
import { Card, CardContent, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { formatAgentCount } from './format-agent-count';
import type { Category } from '../../api/types';

export function CategoryGrid({ categories }: { categories: readonly Category[] }) {
    if (categories.length === 0) {
        return null;
    }

    return (
        <section className="space-y-3">
            <h2 className="text-lg font-semibold">Browse by category</h2>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                {categories.map(category => (
                    <Link
                        key={category.id}
                        to={`/catalog?category=${encodeURIComponent(category.id)}`}
                        aria-label={`${category.name ?? category.id}, ${formatAgentCount(category.total_apis)}`}
                        className="rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                        <Card className="h-full transition-colors hover:bg-accent/40">
                            <CardHeader>
                                <CardTitle className="text-base">{category.name ?? category.id}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="text-sm text-muted-foreground">{formatAgentCount(category.total_apis)}</p>
                            </CardContent>
                        </Card>
                    </Link>
                ))}
            </div>
        </section>
    );
}
