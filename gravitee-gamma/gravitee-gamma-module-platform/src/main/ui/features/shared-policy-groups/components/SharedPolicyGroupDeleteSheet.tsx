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

/** Mirrors classic Console's `removeSharedPolicyGroup` confirm dialog (shared-policy-groups.component.ts). */
export function SharedPolicyGroupDeleteSheet({
    open,
    onClose,
    onConfirm,
    isDeleting,
}: Readonly<{
    open: boolean;
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}>) {
    return (
        <ConfirmDialog
            open={open}
            onOpenChange={isOpen => !isOpen && onClose()}
            title="Remove Shared Policy Group"
            description={
                <span className="space-y-2 block">
                    <span className="block">Are you sure you want to remove this Shared Policy Group?</span>
                    <span className="block">
                        If this Shared Policy Group is used in API flows, be sure to inform API publishers before making this change.
                    </span>
                    <span className="block">
                        If an API flow still uses this Shared Policy Group, the API flow will ignore it and continue to run.
                    </span>
                </span>
            }
            confirmLabel="Remove"
            pendingLabel="Removing…"
            destructive
            isPending={isDeleting}
            onConfirm={onConfirm}
        />
    );
}
