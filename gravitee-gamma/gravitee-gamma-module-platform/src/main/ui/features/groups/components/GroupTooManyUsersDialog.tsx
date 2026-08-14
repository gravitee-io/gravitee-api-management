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

import { Alert, AlertDescription, Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';

export function GroupTooManyUsersDialog({
    open,
    email,
    onClose,
    onContinue,
}: Readonly<{
    open: boolean;
    email: string | null;
    onClose: () => void;
    onContinue: () => void;
}>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Many Users Found</DialogTitle>
                </DialogHeader>
                <div className="space-y-3 px-6">
                    <Alert variant="default">
                        <InfoIcon className="size-4" aria-hidden />
                        <AlertDescription>
                            More than one user already exist with the email id {email} in the organization. You can select and add a
                            specific user using add member function.
                        </AlertDescription>
                    </Alert>
                    <p className="text-sm">Do you want to continue?</p>
                </div>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={onContinue}>
                        Continue
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
