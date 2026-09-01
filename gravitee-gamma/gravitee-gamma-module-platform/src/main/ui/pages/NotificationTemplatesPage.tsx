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

import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';

import { NotificationTemplatesList } from '../features/notification-templates/components/NotificationTemplatesList';
import { useNotificationTemplates } from '../features/notification-templates/hooks/useNotificationTemplates';

export function NotificationTemplatesPage() {
    const { categories, templateCount, customCount, isLoading, isError, refetch } = useNotificationTemplates();

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Templates</h1>
                <p className="text-sm text-muted-foreground">
                    The email and portal notifications this organization sends. Every notification has a built-in default; override the ones
                    you want to word differently.
                </p>
            </div>

            <Alert>
                <InfoIcon className="size-4" aria-hidden />
                <AlertDescription>
                    Templates are written in FreeMarker. Placeholders such as <code>{'${api.name}'}</code> are filled in when the
                    notification is sent, and a template marked <strong>Custom</strong> is using your wording instead of the default.
                </AlertDescription>
            </Alert>

            {isError ? (
                <div className="flex flex-col items-start gap-3 rounded-lg border p-6">
                    <p className="text-sm text-muted-foreground">Failed to load notification templates. Please refresh and try again.</p>
                    <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
                        Try again
                    </Button>
                </div>
            ) : (
                <NotificationTemplatesList
                    categories={categories}
                    isLoading={isLoading}
                    templateCount={templateCount}
                    customCount={customCount}
                />
            )}
        </div>
    );
}
