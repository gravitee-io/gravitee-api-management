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
    Label,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Textarea,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

export function ApplicationSubscriptionEditRequestSheet({
    open,
    initialRequest,
    onOpenChange,
    onSave,
    isLoading,
}: Readonly<{
    open: boolean;
    initialRequest: string;
    onOpenChange: (open: boolean) => void;
    onSave: (request: string) => void;
    isLoading: boolean;
}>) {
    const [request, setRequest] = useState(initialRequest);

    useEffect(() => {
        if (open) setRequest(initialRequest);
    }, [open, initialRequest]);

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Edit subscription message</SheetTitle>
                    <SheetDescription>Update this message while the subscription is pending.</SheetDescription>
                </SheetHeader>

                <div className="space-y-2 px-4 py-2">
                    <Label htmlFor="edit-subscription-request">Publisher message to subscriber</Label>
                    <Textarea id="edit-subscription-request" value={request} onChange={e => setRequest(e.target.value)} rows={4} />
                </div>

                <SheetFooter className="flex-row justify-end border-t gap-2">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!request.trim() || isLoading} onClick={() => onSave(request.trim())}>
                        {isLoading ? 'Saving…' : 'Save changes'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
