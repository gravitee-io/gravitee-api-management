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
import { QueryClient } from '@tanstack/react-query';

function retryTransientRequest(failureCount: number, error: unknown): boolean {
    const status = typeof error === 'object' && error !== null && 'status' in error ? error.status : undefined;
    if (typeof status === 'number' && status >= 400 && status < 500) {
        return false;
    }
    return failureCount < 2;
}

export function createPortalsQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: retryTransientRequest },
        },
    });
}
