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
import { TargetsProvider } from '@gravitee/gamma-lib-observability';
import type { ReactNode } from 'react';

import { useTargetsBaseUrl } from '../../../../lib/hooks/useTargetsBaseUrl';
import { gammaConsoleHttpOptions } from '../../../../shared/api/apimClient';

/**
 * The targets library wired to this console: the management v2 environment root
 * and the authenticated transport every other module call uses. Renders its
 * children before the base URL is known, so a list can show while its badges wait.
 */
export function ApiTargetsProvider({ children }: { readonly children: ReactNode }) {
    const baseUrl = useTargetsBaseUrl();
    return (
        <TargetsProvider baseUrl={baseUrl} http={gammaConsoleHttpOptions}>
            {children}
        </TargetsProvider>
    );
}
