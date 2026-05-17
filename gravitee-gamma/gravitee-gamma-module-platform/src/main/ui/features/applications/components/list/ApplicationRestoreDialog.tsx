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

import type { ApplicationListItem } from '../../types/application';

export function ApplicationRestoreDialog({
    application,
    onClose,
    onConfirm,
    isLoading,
    error,
}: Readonly<{
    application: ApplicationListItem | null;
    onClose: () => void;
    onConfirm: () => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    return (
        <Dialog open={application !== null} onOpenChange={open => !open && onClose()}>
            <DialogContent className="w-full max-w-md sm:max-w-md" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle className="text-lg font-semibold leading-snug">
                        Would you like to restore the application &ldquo;{application?.name}&rdquo;?
                    </DialogTitle>
                    <DialogDescription>
                        Every subscription belonging to this application will be restored in PENDING status. Subscriptions can be
                        reactivated as per requirements.
                    </DialogDescription>
                </DialogHeader>
                {error ? <p className="text-sm text-destructive">{error}</p> : null}
                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button
                        type="button"
                        className="bg-primary text-primary-foreground hover:bg-primary/90"
                        disabled={isLoading}
                        onClick={onConfirm}
                    >
                        {isLoading ? 'Restoring…' : 'Restore'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
