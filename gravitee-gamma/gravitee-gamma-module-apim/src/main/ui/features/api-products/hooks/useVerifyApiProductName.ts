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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';

import { verifyApiProductName } from '../services/apiProduct';
import type { VerifyApiProductNameResponse } from '../types/apiProduct';
import { apiProductKeys } from '../utils/queryKeys';

export function useVerifyApiProductName(
    name: string,
    productId?: string,
): { data: VerifyApiProductNameResponse | undefined; isChecking: boolean } {
    const env = useEnvironment();

    const { data, isFetching } = useQuery<VerifyApiProductNameResponse>({
        queryKey: apiProductKeys.verify(env?.id ?? '', name, productId),
        queryFn: () => verifyApiProductName(env!.id, name, productId),
        enabled: Boolean(env) && name.length >= 1,
    });

    return { data, isChecking: isFetching };
}
