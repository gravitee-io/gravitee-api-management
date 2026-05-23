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
import { createContext, useContext } from 'react';

import type { ApiDetailDto } from '../types';

export type ApiDetailContextValue = {
    readonly api: ApiDetailDto | null;
    readonly isLoading: boolean;
    /** True once the API-scoped permission set has been loaded into `permissionService`. */
    readonly permissionsReady: boolean;
};

export const ApiDetailContext = createContext<ApiDetailContextValue>({ api: null, isLoading: true, permissionsReady: false });

export function useApiDetailContext(): ApiDetailContextValue {
    return useContext(ApiDetailContext);
}
