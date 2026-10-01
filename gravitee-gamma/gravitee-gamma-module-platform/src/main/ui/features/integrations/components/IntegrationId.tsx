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

import { useId } from 'react';

import { CopyableProfileValue } from '../../users/components/CopyableProfileValue';

export function IntegrationId({ integrationId }: Readonly<{ integrationId: string }>) {
    const headingId = useId();

    return (
        <section className="space-y-2" data-testid="integration-id" aria-labelledby={headingId}>
            <h2 id={headingId} className="text-base font-semibold">
                Integration ID
            </h2>
            <CopyableProfileValue value={integrationId} copyAriaLabel="Copy integration ID" className="font-mono text-sm" />
        </section>
    );
}
