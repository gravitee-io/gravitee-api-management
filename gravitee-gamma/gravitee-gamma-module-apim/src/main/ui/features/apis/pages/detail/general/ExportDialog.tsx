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
import { DownloadIcon } from '@gravitee/graphene-core/icons';

export function ExportDialog({
    open,
    onOpenChange,
    onExport,
}: Readonly<{ open: boolean; onOpenChange: (v: boolean) => void; onExport: () => void }>) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>Export API Definition</DialogTitle>
                    <DialogDescription>Download the full API definition as a Gravitee JSON file.</DialogDescription>
                </DialogHeader>
                <div className="py-2">
                    <p className="rounded-md bg-muted/30 border p-3 text-sm text-muted-foreground">
                        The exported file contains the complete API definition. Plans, members, and documentation pages are not included.
                    </p>
                </div>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button
                        type="button"
                        onClick={() => {
                            onExport();
                            onOpenChange(false);
                        }}
                    >
                        <DownloadIcon className="size-4" /> Export
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
