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
import { Badge, Field, FieldDescription, FieldLabel, FieldLegend, FieldSet, Input, Label, Switch } from '@gravitee/graphene-core';
import { FolderOpenIcon } from '@gravitee/graphene-core/icons';
import { type ReactNode, useId } from 'react';

import { ITEM_NOUNS, type ItemType } from './DocumentationItemBadges';
import type { PortalNavigationFolder } from '../../../types/apiDocumentation';

/** The folder holding a page, when it requires authentication: the server then refuses to make the page public. */
export function privateParentOf(parent: PortalNavigationFolder | undefined): PortalNavigationFolder | undefined {
    return parent?.visibility === 'PRIVATE' ? parent : undefined;
}

/** The title and access of a documentation item, shared by the dialogs creating one and editing its details. */
export function DocumentationItemDetailsFields({
    itemType = 'PAGE',
    title,
    onTitleChange,
    isPrivate,
    onPrivateChange,
    privateParent,
    titleReadOnly = false,
    disabled,
    children,
}: Readonly<{
    itemType?: ItemType;
    title: string;
    onTitleChange: (title: string) => void;
    isPrivate: boolean;
    onPrivateChange: (isPrivate: boolean) => void;
    /** Locks the item private, saying why. */
    privateParent?: PortalNavigationFolder;
    titleReadOnly?: boolean;
    disabled: boolean;
    /** Fields shown between the title and the access, such as a link's URL. */
    children?: ReactNode;
}>) {
    const titleId = useId();
    const privateId = useId();
    const itemNoun = ITEM_NOUNS[itemType];

    return (
        <>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor={titleId}>Title</FieldLabel>
                <Input
                    id={titleId}
                    value={title}
                    onChange={event => onTitleChange(event.target.value)}
                    disabled={disabled || titleReadOnly}
                    required
                />
            </Field>

            {children}

            <FieldSet className="gap-1.5">
                <FieldLegend variant="label">Access</FieldLegend>
                <div className="flex items-center gap-2">
                    <Switch
                        id={privateId}
                        checked={isPrivate}
                        onCheckedChange={onPrivateChange}
                        disabled={disabled || privateParent !== undefined}
                    />
                    <Label htmlFor={privateId} className="font-normal">
                        Authentication is required to view this {itemNoun}
                    </Label>
                </div>
                {privateParent ? (
                    <FieldDescription>
                        The parent folder{' '}
                        <Badge variant="outline" className="gap-1 align-middle font-medium text-foreground">
                            <FolderOpenIcon className="size-3" aria-hidden />
                            {privateParent.title}
                        </Badge>{' '}
                        requires authentication, so this {itemNoun} does too.
                    </FieldDescription>
                ) : null}
            </FieldSet>
        </>
    );
}
