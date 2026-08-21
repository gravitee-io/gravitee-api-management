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

import { Button, PageFocused } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { IdentityProviderCreateForm } from '../features/authentication/components/IdentityProviderCreateForm';

export function CreateIdentityProviderPage() {
    const navigate = useNavigate();

    return (
        <PageFocused>
            <div className="space-y-6">
                <div className="space-y-2">
                    <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" onClick={() => navigate('..')}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Authentication
                    </Button>
                    <h1 className="text-2xl font-semibold tracking-tight">Create a new identity provider</h1>
                </div>
                <IdentityProviderCreateForm />
            </div>
        </PageFocused>
    );
}
