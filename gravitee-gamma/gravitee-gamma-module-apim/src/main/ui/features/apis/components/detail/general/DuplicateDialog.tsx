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
    Input,
    Label,
} from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

export function DuplicateDialog({
    open,
    onOpenChange,
    initialName,
    initialVersion,
    onDuplicate,
    isLoading,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    initialName: string;
    initialVersion: string;
    onDuplicate: (opts: { version: string; contextPath: string }) => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    const [version, setVersion] = useState(initialVersion);
    const [contextPath, setContextPath] = useState('');

    useEffect(() => {
        if (open) {
            setVersion(initialVersion);
            setContextPath('');
        }
    }, [open, initialVersion]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ maxWidth: '512px' }}>
                <DialogHeader>
                    <DialogTitle>Duplicate API</DialogTitle>
                    <DialogDescription>
                        Create a copy of <strong>{initialName}</strong>. Subscriptions are not copied.
                    </DialogDescription>
                </DialogHeader>
                <div className="space-y-3 py-2">
                    <div className="space-y-1">
                        <Label htmlFor="dup-version">
                            Version <span className="text-destructive">*</span>
                        </Label>
                        <Input id="dup-version" value={version} onChange={e => setVersion(e.target.value)} placeholder="v1.0.0" />
                    </div>
                    <div className="space-y-1">
                        <Label htmlFor="dup-path">
                            Context Path <span className="text-muted-foreground text-xs font-normal">(optional)</span>
                        </Label>
                        <Input
                            id="dup-path"
                            value={contextPath}
                            onChange={e => setContextPath(e.target.value)}
                            placeholder="/my-api/v1-copy"
                        />
                    </div>
                    {error && <p className="text-sm text-destructive">{error}</p>}
                </div>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button
                        type="button"
                        disabled={!version.trim() || isLoading}
                        onClick={() => onDuplicate({ version: version.trim(), contextPath: contextPath.trim() })}
                    >
                        <CopyIcon className="size-4" /> {isLoading ? 'Duplicating…' : 'Duplicate'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
