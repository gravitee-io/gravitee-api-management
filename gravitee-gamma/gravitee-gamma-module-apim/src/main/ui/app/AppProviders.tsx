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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';

import { ApimRuntimeProvider } from '../core/context/apimRuntimeContext';

/** Shared by standalone shell and federated wizard entry so React Query + APIM runtime are always available. */
export function AppProviders({ children }: Readonly<{ children: ReactNode }>) {
    const [queryClient] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: { retry: 1, refetchOnWindowFocus: false },
                    mutations: { retry: 0 },
                },
            }),
    );
    return (
        <QueryClientProvider client={queryClient}>
            <ApimRuntimeProvider>{children}</ApimRuntimeProvider>
        </QueryClientProvider>
    );
}
