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

import { fetchCurrentUser, hasOrganizationAdminRole } from '../services/currentUser';
import { currentUserKeys } from '../utils/queryKeys';

/**
 * Whether the current user has the organization ADMIN role (required to list archived apps and restore).
 * Aligns with console `gioRole` / `ApplicationResource.restoreApplication` admin check.
 */
export function useOrganizationAdmin(): { isAdmin: boolean; isLoading: boolean } {
    const { data, isLoading } = useQuery({
        queryKey: currentUserKeys.detail(),
        queryFn: fetchCurrentUser,
        staleTime: 300_000,
    });

    return {
        isAdmin: hasOrganizationAdminRole(data?.roles),
        isLoading,
    };
}
