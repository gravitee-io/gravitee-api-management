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

import { useEffect } from 'react';

import { notify } from '../../../shared/notify';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';

export const INTEGRATION_LOAD_ERROR_MESSAGE = 'Integration could not be loaded. Please refresh and try again.';

export function useIntegrationLoadFailure(integrationId: string, isError: boolean, error: unknown): boolean {
    const isForbidden = isForbiddenApiError(isError, error);

    useEffect(() => {
        if (!isError || isForbidden) return;
        notify.error(error, INTEGRATION_LOAD_ERROR_MESSAGE);
    }, [error, isError, isForbidden]);

    useEffect(() => {
        if (!isForbidden) return;
        console.warn(
            `Integration ${integrationId} was denied (403) although its permissions grant definition read; redirecting to the Integrations list`,
            error,
        );
    }, [isForbidden, error, integrationId]);

    return isForbidden;
}
