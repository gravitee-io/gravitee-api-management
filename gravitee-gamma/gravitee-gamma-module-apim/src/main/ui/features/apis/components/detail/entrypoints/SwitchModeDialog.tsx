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

export function SwitchModeDialog({ open, onConfirm, onCancel }: Readonly<{ open: boolean; onConfirm: () => void; onCancel: () => void }>) {
    return (
        <Dialog open={open} onOpenChange={v => !v && onCancel()}>
            <DialogContent style={{ maxWidth: '440px' }}>
                <DialogHeader>
                    <DialogTitle>Switch to context-path mode</DialogTitle>
                    <DialogDescription>
                        By moving back to context-path mode you will lose all virtual-host configuration. The paths you entered will be
                        preserved. Are you sure you want to continue?
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline" onClick={onCancel}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button variant="destructive" onClick={onConfirm}>
                        Switch
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
