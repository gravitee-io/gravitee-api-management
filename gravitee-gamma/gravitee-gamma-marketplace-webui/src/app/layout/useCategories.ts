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
import { useEffect, useState } from 'react';

import { portalApi } from '../../api/portal-client';
import type { CategoriesResponse, Category } from '../../api/types';

export function useCategories(): Category[] {
    const [categories, setCategories] = useState<Category[]>([]);

    useEffect(() => {
        let cancelled = false;

        portalApi
            .get<CategoriesResponse>('/apis/categories')
            .then(response => {
                if (!cancelled) {
                    setCategories(response.data ?? []);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setCategories([]);
                }
            });

        return () => {
            cancelled = true;
        };
    }, []);

    return categories;
}
