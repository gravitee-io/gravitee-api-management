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
import {
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';

import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';

export function ApplicationSubscriptionRevokeApiKeyDialog({
    apiKey,
    onClose,
    onConfirm,
    isLoading,
    error,
}: Readonly<{
    apiKey: ApplicationSubscriptionApiKeyRow | null;
    onClose: () => void;
    onConfirm: () => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    return (
        <Dialog open={Boolean(apiKey)} onOpenChange={open => !open && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Revoke API key?</DialogTitle>
                    <DialogDescription>
                        {apiKey ? (
                            <>
                                Revoke key <code className="text-foreground">{apiKey.maskedKey}</code>? This cannot be undone without
                                renewing.
                            </>
                        ) : null}
                    </DialogDescription>
                </DialogHeader>
                {error ? <p className="text-sm text-destructive">{error}</p> : null}
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isLoading}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" variant="destructive" disabled={isLoading} onClick={onConfirm}>
                        {isLoading ? 'Revoking…' : 'Revoke'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
