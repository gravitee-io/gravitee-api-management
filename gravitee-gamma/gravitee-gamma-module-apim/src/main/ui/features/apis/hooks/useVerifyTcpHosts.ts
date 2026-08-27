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
import { verifyApiHosts } from '../services/apiProxy';
import { useApiCreation } from '../store/apiCreationStore';
import { validateTcpHosts } from '../utils/apiCreationValidation';
import { isTcpForm } from '../utils/protocol';

/**
 * Watches `form.tcpHosts` and, while protocol is TCP, fires a debounced uniqueness
 * check against the gateway (matching legacy console webui's hostAsyncValidator).
 * Writes the result directly into store validation errors.
 */
export function useVerifyTcpHosts() {
    const { state, dispatch } = useApiCreation();
    const { tcpHosts } = state.form;

    useDebouncedUniquenessCheck({
        depsKey: tcpHosts.map(h => h.host).join(','),
        skip: !isTcpForm(state.form) || validateTcpHosts(tcpHosts) !== null,
        field: 'tcpHosts',
        fallbackMessage: 'One of these hosts is already in use by another API.',
        onVerified: () => dispatch({ type: 'UPDATE_FORM', patch: { tcpHosts } }),
        verify: environmentId => verifyApiHosts(environmentId, 'TCP', tcpHosts.map(h => h.host.trim()).filter(Boolean)),
    });
}
