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
import { useNavigate, useParams } from 'react-router-dom';

import { ApiProxyWizard } from '../features/apis/components/wizard/ApiProxyWizard';
import { ApiCreationProvider } from '../features/apis/store/apiCreationStore';
import { PROXY_TEMPLATES } from '../features/apis/templates/proxyTemplates';

export function TemplateWizardPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const template = PROXY_TEMPLATES.find(t => t.id === id);

    useEffect(() => {
        if (!template) navigate('..', { replace: true });
    }, [template, navigate]);

    if (!template) return null;

    return (
        <ApiCreationProvider initialTemplate={template}>
            <ApiProxyWizard mode="template" />
        </ApiCreationProvider>
    );
}
