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
import { Button } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { RegisterApplicationForm } from '../features/applications/components/create';

export function RegisterApplicationPage() {
    const navigate = useNavigate();

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-3">
                <Button type="button" variant="ghost" size="icon" onClick={() => navigate('..')} aria-label="Back to applications">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                </Button>
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Application creation</h1>
                    <p className="text-sm text-muted-foreground">
                        An application is required to subscribe to API&apos;s plan. It is the intermediate link between a user (you) and an
                        API managed by someone else.
                    </p>
                </div>
            </div>

            <RegisterApplicationForm />
        </div>
    );
}
