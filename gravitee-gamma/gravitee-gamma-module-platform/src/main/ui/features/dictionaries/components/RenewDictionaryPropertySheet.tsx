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
    Field,
    FieldLabel,
    Input,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

export function RenewDictionaryPropertySheet({
    open,
    propertyKey,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    propertyKey: string | undefined;
    onClose: () => void;
    onSubmit: (renewal: { key: string; value: string }) => Promise<void>;
    isSaving: boolean;
}>) {
    const [value, setValue] = useState('');

    useEffect(() => {
        if (open) setValue('');
    }, [open]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const canSubmit = propertyKey !== undefined && value.trim().length > 0 && !isSaving;

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!propertyKey || !canSubmit) return;
        await onSubmit({ key: propertyKey, value });
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Replace Value</SheetTitle>
                    <SheetDescription>Enter a new plaintext value. It will be re-encrypted on save.</SheetDescription>
                </SheetHeader>

                <form id="renew-dictionary-property-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                    <div className="space-y-1">
                        <p className="text-xs font-medium text-muted-foreground">Key</p>
                        <p className="font-mono text-sm font-medium">{propertyKey}</p>
                    </div>
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="renew-property-value">
                            Value <span className="text-destructive">*</span>
                        </FieldLabel>
                        <Input
                            id="renew-property-value"
                            type="password"
                            autoComplete="off"
                            value={value}
                            onChange={e => setValue(e.target.value)}
                            disabled={isSaving}
                            required
                        />
                    </Field>
                </form>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="renew-dictionary-property-form" disabled={!canSubmit}>
                        {isSaving ? 'Replacing…' : 'Replace'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
