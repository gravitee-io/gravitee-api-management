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
import { validateContextPath, validateVirtualHostPath } from '../utils/apiCreationValidation';

/**
 * Watches the context path, or each virtual host, and fires a debounced uniqueness
 * check against `POST /apis/_verify/paths` (Classic does the same before Next).
 */
export function useVerifyContextPath() {
    const { state, dispatch } = useApiCreation();
    const { contextPath, virtualHostsEnabled, virtualHosts } = state.form;
    const virtualHostKey = virtualHosts.map(vh => `${vh.host}\n${vh.path}`).join('\n');
    const virtualHostsReady =
        virtualHosts.length > 0 && virtualHosts.every(vh => vh.host.trim() !== '' && validateVirtualHostPath(vh.path) === null);

    useDebouncedUniquenessCheck({
        depsKey: virtualHostsEnabled ? virtualHostKey : contextPath,
        skip: virtualHostsEnabled ? !virtualHostsReady : validateContextPath(contextPath) !== null,
        field: virtualHostsEnabled ? 'virtualHosts' : 'contextPath',
        fallbackMessage: 'This context path is already in use by another API.',
        onVerified: () =>
            dispatch({
                type: 'CLEAR_FIELD_ERROR',
                field: virtualHostsEnabled ? 'virtualHosts' : 'contextPath',
            }),
        verify: environmentId =>
            verifyContextPath(
                environmentId,
                virtualHostsEnabled ? virtualHosts.map(vh => ({ path: vh.path, host: vh.host })) : [{ path: contextPath }],
            ),
    });
}
