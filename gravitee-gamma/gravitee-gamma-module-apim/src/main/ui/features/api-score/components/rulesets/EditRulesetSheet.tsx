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
    Input,
    Label,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Textarea,
} from '@gravitee/graphene-core';
import { useId, useState } from 'react';

import { PayloadPreview } from './PayloadPreview';
import type { ScoringRuleset, UpdateRulesetRequest } from '../../types/apiScore';
import { RULESET_DESCRIPTION_MAX, RULESET_NAME_MAX } from '../../utils/scoreFormat';

interface EditRulesetSheetProps {
    ruleset: ScoringRuleset | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onSubmit: (rulesetId: string, request: UpdateRulesetRequest) => void;
    isSubmitting?: boolean;
}

export function EditRulesetSheet({ ruleset, open, onOpenChange, onSubmit, isSubmitting }: EditRulesetSheetProps) {
    const nameId = useId();
    const descriptionId = useId();

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [prevRulesetId, setPrevRulesetId] = useState<string | null>(null);

    // Sync fields when a different ruleset is opened for editing.
    if (ruleset && ruleset.id !== prevRulesetId) {
        setPrevRulesetId(ruleset.id);
        setName(ruleset.name);
        setDescription(ruleset.description ?? '');
    }

    const nameValid = name.trim().length > 0 && name.length <= RULESET_NAME_MAX;
    const descriptionValid = description.length <= RULESET_DESCRIPTION_MAX;
    const canSubmit = Boolean(ruleset) && nameValid && descriptionValid && !isSubmitting;

    const handleSubmit = () => {
        if (!ruleset) return;
        onSubmit(ruleset.id, { name: name.trim(), description: description.trim() });
    };

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" className="max-w-lg">
                <SheetHeader>
                    <SheetTitle>Edit Ruleset</SheetTitle>
                    <SheetDescription>Update the name and description. The ruleset content and format cannot be changed.</SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto px-4">
                    <div className="space-y-2">
                        <Label htmlFor={nameId}>Name</Label>
                        <Input id={nameId} value={name} onChange={e => setName(e.target.value)} maxLength={RULESET_NAME_MAX} />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={descriptionId}>Description</Label>
                        <Textarea
                            id={descriptionId}
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                            maxLength={RULESET_DESCRIPTION_MAX}
                            rows={3}
                        />
                        <p className="text-right text-xs text-muted-foreground">
                            {description.length}/{RULESET_DESCRIPTION_MAX}
                        </p>
                    </div>

                    {ruleset && (
                        <div className="space-y-2">
                            <Label>Content</Label>
                            <PayloadPreview payload={ruleset.payload} />
                        </div>
                    )}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!canSubmit} onClick={handleSubmit}>
                        {isSubmitting ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
