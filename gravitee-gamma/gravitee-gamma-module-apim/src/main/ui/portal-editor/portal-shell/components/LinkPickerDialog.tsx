/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';

import type { PortalNavigationPage } from '../../portals/types';
import { NavLinkPagePicker } from './NavLinkPagePicker';

interface LinkPickerDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly onSelect: (page: PortalNavigationPage) => void;
}

export function LinkPickerDialog({ open, onOpenChange, portalPages, onSelect }: LinkPickerDialogProps) {
    const handleSelect = (page: PortalNavigationPage) => {
        onSelect(page);
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-auto max-w-sm">
                <DialogHeader>
                    <DialogTitle>Link to page</DialogTitle>
                    <DialogDescription>Search for a portal page to link to.</DialogDescription>
                </DialogHeader>
                <NavLinkPagePicker
                    pages={portalPages}
                    onSelect={handleSelect}
                    onCancel={() => onOpenChange(false)}
                />
            </DialogContent>
        </Dialog>
    );
}
