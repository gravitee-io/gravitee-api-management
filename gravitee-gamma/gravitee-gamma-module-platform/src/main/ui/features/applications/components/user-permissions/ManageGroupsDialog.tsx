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
    Badge,
    Button,
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';

import type { EnvironmentGroup } from '../../types/applicationMembers.types';

export function ManageGroupsDialog({
    open,
    allGroups,
    currentGroupIds,
    onClose,
    onSave,
    isSaving,
}: Readonly<{
    open: boolean;
    allGroups: EnvironmentGroup[];
    currentGroupIds: string[];
    onClose: () => void;
    onSave: (groupIds: string[]) => void;
    isSaving: boolean;
}>) {
    const [selected, setSelected] = useState<Set<string>>(() => new Set(currentGroupIds));
    const [search, setSearch] = useState('');

    // Reset selection and search each time the dialog opens (setState-during-render pattern).
    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setSelected(new Set(currentGroupIds));
            setSearch('');
        }
    }

    const handleOpen = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return query ? allGroups.filter(g => g.name.toLowerCase().includes(query)) : allGroups;
    }, [allGroups, search]);

    function toggle(id: string) {
        setSelected(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    return (
        <Dialog open={open} onOpenChange={handleOpen}>
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Manage groups</DialogTitle>
                    <DialogDescription>Select the groups that should have access to this application.</DialogDescription>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="relative">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                        <Input
                            placeholder="Search groups…"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            style={{ paddingLeft: '2.5rem' }}
                        />
                    </div>

                    <div className="overflow-y-auto rounded-md" style={{ maxHeight: '18rem' }}>
                        {filtered.length === 0 ? (
                            <p className="p-3 text-sm text-muted-foreground">
                                {search.trim() ? 'No groups match your search.' : 'No groups found in this environment.'}
                            </p>
                        ) : (
                            <div className="space-y-1">
                                {filtered.map(group => {
                                    const isAssociated = currentGroupIds.includes(group.id);
                                    const isChecked = selected.has(group.id);
                                    return (
                                        <label
                                            key={group.id}
                                            htmlFor={`group-${group.id}`}
                                            className="flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-muted/50 cursor-pointer"
                                        >
                                            <Checkbox
                                                id={`group-${group.id}`}
                                                checked={isChecked}
                                                onCheckedChange={() => toggle(group.id)}
                                            />
                                            <span className="flex-1 text-sm select-none">{group.name}</span>
                                            {isAssociated ? (
                                                <Badge variant="secondary" className="text-xs">
                                                    Associated
                                                </Badge>
                                            ) : null}
                                        </label>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    <p className="text-xs text-muted-foreground">
                        {selected.size} group{selected.size !== 1 ? 's' : ''} selected
                    </p>
                </div>

                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={() => handleOpen(false)} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => onSave([...selected])} disabled={isSaving}>
                        Save
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
