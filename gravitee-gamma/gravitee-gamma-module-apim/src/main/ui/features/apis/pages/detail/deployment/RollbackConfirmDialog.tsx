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
import { Button, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';

type Props = Readonly<{
    version: string;
    isRollingBack: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}>;

export function RollbackConfirmDialog({ version, isRollingBack, onConfirm, onCancel }: Props) {
    return (
        <Dialog open onOpenChange={open => !open && !isRollingBack && onCancel()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Rollback to v{version}?</DialogTitle>
                    <DialogDescription>
                        This will update the API to version {version}. The API stays out of sync until you deploy it. This action cannot be
                        undone.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="gap-2 sm:justify-end">
                    <Button variant="outline" size="sm" disabled={isRollingBack} onClick={onCancel}>
                        Cancel
                    </Button>
                    <Button size="sm" variant="destructive" disabled={isRollingBack} onClick={onConfirm}>
                        {isRollingBack ? 'Rolling back…' : 'Confirm rollback'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
