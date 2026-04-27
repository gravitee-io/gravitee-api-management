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
import { Button, Card } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

type CreatedLocationState = {
    deployed?: boolean;
    warnings?: string[];
};

/** Placeholder detail route after proxy creation until full API studio ships in Gamma. */
export function ApiCreatedPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();
    const location = useLocation();
    const state = location.state as CreatedLocationState | undefined;
    const warnings = state?.warnings?.filter(Boolean) ?? [];
    const deployed = state?.deployed;

    return (
        <div className="space-y-6">
            <div className="space-y-2">
                <h1 className="text-2xl font-semibold tracking-tight">API created</h1>
                <p className="text-sm text-muted-foreground">
                    Your API was created successfully. Full management screens will be available here later.
                </p>
                {deployed !== undefined ? (
                    <p className="text-sm text-muted-foreground">
                        Gateway state:{' '}
                        <span className="font-medium text-foreground">{deployed ? 'started (deployed)' : 'created (not started)'}</span>
                    </p>
                ) : null}
            </div>
            {warnings.length > 0 ? (
                <Card className="rounded-xl border border-destructive/25 bg-destructive/5 p-4">
                    <p className="text-sm font-medium text-destructive">Warnings</p>
                    <ul className="mt-2 list-inside list-disc text-sm text-muted-foreground">
                        {warnings.map((w, i) => (
                            <li key={i}>{w}</li>
                        ))}
                    </ul>
                </Card>
            ) : null}
            <Card className="rounded-xl p-4 sm:p-6">
                <p className="text-sm font-medium text-foreground">API id</p>
                <p className="mt-1 font-mono text-sm break-all text-muted-foreground">{apiId}</p>
            </Card>
            <Button type="button" variant="outline" size="sm" className="gap-2" onClick={() => navigate('..')}>
                <ArrowLeftIcon className="size-4" aria-hidden="true" />
                Back to API list
            </Button>
        </div>
    );
}
