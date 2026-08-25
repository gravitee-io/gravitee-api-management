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
    ScrollArea,
} from '@gravitee/graphene-core';

import { WIDE_DIALOG_CONTENT_STYLE, WIDE_DIALOG_STYLE } from '../../../shared/layout/dialogLayout';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

interface SharedPolicyGroupHistoryJsonDialogProps {
    readonly sharedPolicyGroup?: SharedPolicyGroup;
    readonly onOpenChange: (open: boolean) => void;
}

export function SharedPolicyGroupHistoryJsonDialog({ sharedPolicyGroup, onOpenChange }: SharedPolicyGroupHistoryJsonDialogProps) {
    if (!sharedPolicyGroup) {
        return null;
    }

    return (
        <Dialog open onOpenChange={onOpenChange}>
            <DialogContent style={WIDE_DIALOG_STYLE}>
                <DialogHeader>
                    <DialogTitle>Version {sharedPolicyGroup.version ?? '—'} JSON Source</DialogTitle>
                    <DialogDescription>Read-only JSON source for this deployed Shared Policy Group version.</DialogDescription>
                </DialogHeader>
                <ScrollArea className="rounded-lg border bg-muted/30" style={WIDE_DIALOG_CONTENT_STYLE}>
                    <pre aria-label="Shared Policy Group JSON source" className="min-w-max p-4 font-mono text-xs leading-5">
                        {JSON.stringify(sharedPolicyGroup, null, 2)}
                    </pre>
                </ScrollArea>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button">Close</Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
