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
    Input,
    ScrollArea,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';

import type { Group } from '../../types/members.types';

export function ManageGroups({
    open,
    allGroups,
    currentGroupIds,
    onClose,
    onSave,
    isSaving,
}: Readonly<{
    open: boolean;
    allGroups: Group[];
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
        <Sheet open={open} onOpenChange={handleOpen}>
            <SheetContent side="right" style={{ maxWidth: '28rem' }}>
                <SheetHeader>
                    <SheetTitle>Manage groups</SheetTitle>
                    <SheetDescription>Select the groups that should have access to this API.</SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 px-4">
                    <div className="relative">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                        <Input className="pl-9" placeholder="Search groups…" value={search} onChange={e => setSearch(e.target.value)} />
                    </div>

                    <ScrollArea className="min-h-0 flex-1 rounded-md">
                        {filtered.length === 0 ? (
                            <p className="p-3 text-sm text-muted-foreground">
                                {search.trim() ? 'No groups match your search.' : 'No groups found in this environment.'}
                            </p>
                        ) : (
                            <div className="space-y-1 pr-3">
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
                    </ScrollArea>

                    <p className="text-xs text-muted-foreground">
                        {selected.size} group{selected.size !== 1 ? 's' : ''} selected
                    </p>
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => handleOpen(false)} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => onSave([...selected])} disabled={isSaving}>
                        Save
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
