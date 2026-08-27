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
import { useDebouncedUniquenessCheck } from './useDebouncedUniquenessCheck';
import { verifyContextPath } from '../services/apiProxy';
import { useApiCreation } from '../store/apiCreationStore';
import { validateContextPath } from '../utils/apiCreationValidation';

/**
 * Watches `form.contextPath` and, when virtual hosts are disabled, fires a
 * debounced uniqueness check against the gateway (matching legacy console
 * webui behaviour). Writes the result directly into store validation errors.
 */
export function useVerifyContextPath() {
    const { state, dispatch } = useApiCreation();
    const { contextPath, virtualHostsEnabled } = state.form;

    useDebouncedUniquenessCheck({
        depsKey: contextPath,
        skip: virtualHostsEnabled || validateContextPath(contextPath) !== null,
        field: 'contextPath',
        fallbackMessage: 'This context path is already in use by another API.',
        onVerified: () => dispatch({ type: 'UPDATE_FORM', patch: { contextPath } }),
        verify: environmentId => verifyContextPath(environmentId, [{ path: contextPath }]),
    });
}
