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

import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

import { A2aIntegrationForm } from './A2aIntegrationForm';
import { notify } from '../../../shared/notify';
import { useCreateIntegration } from '../hooks/useCreateIntegration';
import type { A2aIntegrationFormValues } from '../utils/a2aIntegrationForm';
import { A2A_PROVIDER } from '../utils/integrationKind';

export function CreateA2aIntegration({
    onCancel,
    onSubmittingChange,
}: Readonly<{ onCancel: () => void; onSubmittingChange?: (isSubmitting: boolean) => void }>) {
    const navigate = useNavigate();
    const createIntegration = useCreateIntegration();

    // isPending reaches React one macrotask after mutateAsync starts, so only a ref blocks a second submit in the same task.
    const submitInFlight = useRef(false);

    useEffect(() => {
        onSubmittingChange?.(createIntegration.isPending);
    }, [createIntegration.isPending, onSubmittingChange]);

    async function handleSubmit({ name, description, wellKnownUrls }: A2aIntegrationFormValues) {
        if (submitInFlight.current) return;
        submitInFlight.current = true;
        try {
            const created = await createIntegration.mutateAsync({ name, description, provider: A2A_PROVIDER, wellKnownUrls });
            notify.success(`Integration ${name} created successfully`);
            navigate(`../${created.id}`);
        } catch (error) {
            console.error('Failed to create A2A integration', error);
            notify.error('An error occurred. Integration not created');
        } finally {
            submitInFlight.current = false;
        }
    }

    return (
        <A2aIntegrationForm onSubmit={values => void handleSubmit(values)} onCancel={onCancel} isSubmitting={createIntegration.isPending} />
    );
}
