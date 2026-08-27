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
import type { ApiProxyDraft } from '../types/apiCreation';

/**
 * Single source of truth for "is this creation-wizard draft a TCP proxy" — was previously
 * re-derived as `form.protocol === 'TCP'` independently in half a dozen files. For the
 * post-creation API detail side, the equivalent predicate is `hasTcpListeners()` in
 * `apiHttpProxy.ts` (a different data shape — `ApiDetailDto`, not a wizard draft).
 */
export function isTcpForm(form: Pick<ApiProxyDraft, 'protocol'>): boolean {
    return form.protocol === 'TCP';
}
