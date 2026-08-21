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

import { useQuery } from '@tanstack/react-query';

import { listActivatedIdentityProviders, listIdentityProviders } from '../services/identityProviders';
import { authenticationKeys } from '../utils/queryKeys';

export function useAuthenticationPage() {
    const providersQuery = useQuery({
        queryKey: authenticationKeys.providers(),
        queryFn: listIdentityProviders,
    });
    const activationsQuery = useQuery({
        queryKey: authenticationKeys.activations(),
        queryFn: listActivatedIdentityProviders,
    });
    return { providersQuery, activationsQuery };
}
