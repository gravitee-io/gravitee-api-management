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
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { RadioIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { BroadcastEmptyState } from '../features/broadcasts/components/BroadcastEmptyState';
import { BroadcastForm } from '../features/broadcasts/components/BroadcastForm';
import { BroadcastSuccessBanner } from '../features/broadcasts/components/BroadcastSuccessBanner';
import { useEnvironmentRoles, useSendEnvironmentBroadcast } from '../features/broadcasts/hooks/useEnvironmentBroadcast';
import type { BroadcastPayload } from '../features/broadcasts/types';
import { extractErrorMessage } from '../shared/notify/extractErrorMessage';

type PageState = 'idle' | 'composing' | 'sent';

export function BroadcastsPage() {
    const canSend = useHasPermission({ anyOf: ['environment-message-c'] });
    const { recipientOptions, isLoading: isLoadingRecipients, isError: isRolesError } = useEnvironmentRoles();
    const sendMutation = useSendEnvironmentBroadcast();

    const [pageState, setPageState] = useState<PageState>('idle');
    const [reachCount, setReachCount] = useState(0);

    const handleSend = useCallback(
        (payload: BroadcastPayload) => {
            sendMutation.mutate(payload, {
                onSuccess: reach => {
                    setReachCount(reach);
                    setPageState('sent');
                },
            });
        },
        [sendMutation],
    );

    const handleReturnToIdle = useCallback(() => {
        sendMutation.reset();
        setPageState('idle');
    }, [sendMutation]);

    return (
        <div className="flex flex-col gap-6">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Broadcasts</h1>
                    <p className="text-sm text-muted-foreground">Send announcements to members of this environment.</p>
                </div>
                {canSend && pageState === 'idle' && (
                    <Button
                        type="button"
                        size="sm"
                        disabled={isRolesError || sendMutation.isPending}
                        onClick={() => setPageState('composing')}
                    >
                        <RadioIcon className="size-4" aria-hidden="true" />
                        Compose broadcast
                    </Button>
                )}
            </div>

            {isRolesError && (
                <Alert variant="destructive">
                    <AlertDescription>Failed to load recipient options. Please refresh the page and try again.</AlertDescription>
                </Alert>
            )}

            {pageState === 'idle' && <BroadcastEmptyState />}

            {pageState === 'sent' && <BroadcastSuccessBanner reach={reachCount} onComposeAnother={handleReturnToIdle} />}

            {pageState === 'composing' && (
                <BroadcastForm
                    recipientOptions={recipientOptions}
                    isLoadingRecipients={isLoadingRecipients}
                    isPending={sendMutation.isPending}
                    error={sendMutation.error ? extractErrorMessage(sendMutation.error, 'Failed to send broadcast.') : null}
                    onSend={handleSend}
                    onCancel={handleReturnToIdle}
                />
            )}
        </div>
    );
}
