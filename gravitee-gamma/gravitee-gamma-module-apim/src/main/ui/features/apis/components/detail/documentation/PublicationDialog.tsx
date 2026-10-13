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
    Alert,
    AlertDescription,
    Button,
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { AlertCircleIcon, EyeOffIcon, GlobeIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';

import { ITEM_NOUNS } from './DocumentationItemBadges';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useChangeApiDocumentationItem } from '../../../hooks/useApiDocumentation';
import type { ApiDocumentationItem } from '../../../types/apiDocumentation';

interface PublicationDialogProps {
    apiId: string;
    /** The item to publish when it is unpublished, or to unpublish when it is published. */
    item: ApiDocumentationItem;
    /** How many items a folder holds, at any depth. */
    itemsInside: number;
    onClose: () => void;
}

export function PublicationDialog({ open, ...props }: Readonly<PublicationDialogProps & { open: boolean }>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && props.onClose()}>
            <DialogContent showCloseButton={false}>
                <PublicationForm {...props} />
            </DialogContent>
        </Dialog>
    );
}

function itemsInsideLabel(count: number): string {
    return count === 1 ? 'the 1 item inside' : `the ${count} items inside`;
}

function describeChange(item: ApiDocumentationItem, itemsInside: number): string {
    const noun = ITEM_NOUNS[item.type];
    if (!item.published) {
        return `The ${noun} will become visible in the developer portal.`;
    }
    const hidden = itemsInside > 0 ? `The ${noun} and ${itemsInsideLabel(itemsInside)} it` : `The ${noun}`;
    return `${hidden} will become invisible in the developer portal.`;
}

// Rendered inside the dialog content, so its state starts over each time the dialog opens.
function PublicationForm({ apiId, item, itemsInside, onClose }: Readonly<PublicationDialogProps>) {
    const changeItem = useChangeApiDocumentationItem(apiId);
    const checkboxId = useId();
    const [publishContents, setPublishContents] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);

    const publishing = !item.published;
    const noun = ITEM_NOUNS[item.type];
    // Unpublishing a folder always unpublishes its contents, so only publishing one leaves a choice.
    const offersContents = publishing && itemsInside > 0;
    const action = publishing ? 'Publish' : 'Unpublish';

    async function handleConfirm() {
        if (isSaving) return;
        setIsSaving(true);
        setRefusal(null);
        try {
            await changeItem.mutateAsync({
                navId: item.id,
                changes: { published: publishing },
                propagatePublishToChildren: offersContents ? publishContents : undefined,
            });
            notify.success(`${noun.charAt(0).toUpperCase()}${noun.slice(1)} '${item.title}' ${publishing ? 'published' : 'unpublished'}`);
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
            setIsSaving(false);
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>{`${action} "${item.title}"?`}</DialogTitle>
                <DialogDescription>{describeChange(item, itemsInside)}</DialogDescription>
            </DialogHeader>

            {refusal ? (
                <Alert variant="destructive">
                    <AlertCircleIcon aria-hidden />
                    <AlertDescription>{refusal}</AlertDescription>
                </Alert>
            ) : null}

            {offersContents ? (
                <div className="flex items-center gap-3">
                    <Checkbox
                        id={checkboxId}
                        checked={publishContents}
                        onCheckedChange={checked => setPublishContents(checked === true)}
                        disabled={isSaving}
                    />
                    <label htmlFor={checkboxId} className="text-sm">
                        Also publish {itemsInsideLabel(itemsInside)}
                    </label>
                </div>
            ) : null}

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="button" onClick={handleConfirm} disabled={isSaving}>
                    {publishing ? <GlobeIcon className="size-4" aria-hidden /> : <EyeOffIcon className="size-4" aria-hidden />}
                    {isSaving ? `${action}ing…` : action}
                </Button>
            </DialogFooter>
        </>
    );
}
