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

import { Button, ScrollArea, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { useActionState, useState } from 'react';

import { SharedPolicyGroupBasicFields } from './SharedPolicyGroupBasicFields';
import { FormActionSubmitButton } from '../../../shared/components/FormActionSubmitButton';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { toReadableApiType, toReadableFlowPhase, type SharedPolicyGroup } from '../types/sharedPolicyGroup';
import type { SharedPolicyGroupBasicFormValues } from '../utils/sharedPolicyGroupPayload';

export type SharedPolicyGroupEditFormValues = SharedPolicyGroupBasicFormValues;

function toFormValues(sharedPolicyGroup: SharedPolicyGroup): SharedPolicyGroupEditFormValues {
    return {
        name: sharedPolicyGroup.name,
        description: sharedPolicyGroup.description ?? '',
        prerequisiteMessage: sharedPolicyGroup.prerequisiteMessage ?? '',
    };
}

export function SharedPolicyGroupEditSheet({
    open,
    sharedPolicyGroup,
    onClose,
    onSubmit,
}: Readonly<{
    open: boolean;
    sharedPolicyGroup: SharedPolicyGroup;
    onClose: () => void;
    onSubmit: (values: SharedPolicyGroupEditFormValues) => Promise<void> | void;
}>) {
    const [form, setForm] = useState<SharedPolicyGroupEditFormValues>(() => toFormValues(sharedPolicyGroup));

    function setField<K extends keyof SharedPolicyGroupEditFormValues>(key: K, value: SharedPolicyGroupEditFormValues[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    const isValid = form.name.trim() !== '';
    const [, submitSharedPolicyGroup, isSaving] = useActionState<null, FormData>(async () => {
        if (isValid) {
            await onSubmit({ ...form, name: form.name.trim() });
        }
        return null;
    }, null);

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && !isSaving && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Edit Shared Policy Group</SheetTitle>
                    <SheetDescription>Update the name, description, and prerequisite message for this policy group.</SheetDescription>
                </SheetHeader>

                <form action={submitSharedPolicyGroup} className="flex min-h-0 flex-1 flex-col">
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="flex flex-col gap-5 px-4 py-4">
                            <SharedPolicyGroupBasicFields idPrefix="spg-edit" values={form} disabled={isSaving} onChange={setField} />

                            <dl className="grid grid-cols-2 gap-4 rounded-lg border bg-muted/30 p-3">
                                <div className="space-y-1">
                                    <dt className="text-xs font-medium text-muted-foreground">API type</dt>
                                    <dd className="text-sm">{toReadableApiType(sharedPolicyGroup.apiType)}</dd>
                                </div>
                                <div className="space-y-1">
                                    <dt className="text-xs font-medium text-muted-foreground">Phase</dt>
                                    <dd className="text-sm">{toReadableFlowPhase(sharedPolicyGroup.phase)}</dd>
                                </div>
                            </dl>
                        </div>
                    </ScrollArea>

                    <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                        <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                            Cancel
                        </Button>
                        <FormActionSubmitButton disabled={!isValid} label="Save" pendingLabel="Saving…" />
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    );
}
