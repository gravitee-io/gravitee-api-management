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

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';

/** Mirrors classic group.component.ts's "Add Group To Existing APIs/API Products/Applications" confirm
 *  dialogs — same title/body/button copy, one component parametrized by the target type label. */
export function GroupAssociateDialog({
    open,
    typeLabel,
    onClose,
    onConfirm,
    isAssociating,
}: Readonly<{
    open: boolean;
    typeLabel: string;
    onClose: () => void;
    onConfirm: () => void;
    isAssociating: boolean;
}>) {
    return (
        <ConfirmDialog
            open={open}
            onOpenChange={isOpen => !isOpen && onClose()}
            title={`Add group to existing ${typeLabel}`}
            description={`You are trying to add the group to all the existing ${typeLabel.toLowerCase()}. Do you want to continue?`}
            confirmLabel="Continue"
            pendingLabel="Adding…"
            isPending={isAssociating}
            onConfirm={onConfirm}
        />
    );
}
