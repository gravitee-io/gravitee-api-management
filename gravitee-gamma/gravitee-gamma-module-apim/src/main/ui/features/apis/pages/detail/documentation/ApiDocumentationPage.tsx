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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, Skeleton, useLayoutConfig } from '@gravitee/graphene-core';
import { useParams } from 'react-router-dom';

import { ApiDocumentationWorkspace } from '../../../../api-documentation/components/ApiDocumentationWorkspace';
import { mapApiDetailToPortalApi } from '../../../../api-documentation/utils/map-api-detail-to-portal-api';
import { useApiDetailContext } from '../../../context/ApiDetailContext';

const STANDALONE_EDITOR_BASE_URL = '/portal-editor';

export function ApiDocumentationPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const { api, isLoading } = useApiDetailContext();
    const canRead = useHasPermission({ anyOf: ['api-documentation-r'] });

    useLayoutConfig({ contentVariant: 'full-bleed' }, []);

    if (!canRead) {
        return (
            <Alert variant="destructive">
                <AlertDescription>You do not have permission to view API documentation.</AlertDescription>
            </Alert>
        );
    }

    if (isLoading || !apiId) {
        return (
            <div className="flex flex-col gap-4 p-6">
                <Skeleton className="h-8 w-64" />
                <Skeleton className="h-[480px] w-full" />
            </div>
        );
    }

    return (
        <ApiDocumentationWorkspace
            apiId={apiId}
            apiName={api?.name ?? 'API'}
            apiContext={api ? mapApiDetailToPortalApi(api) : null}
            standaloneEditorBaseUrl={STANDALONE_EDITOR_BASE_URL}
        />
    );
}
