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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, MoreHorizontalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import type { ApiResource } from '../../../../apis/types/resource';

interface ApiProductResourceRowActionsProps {
    resource: ApiResource;
    enabled: boolean;
    isPending: boolean;
    onEdit: (resourceName: string) => void;
    onToggle: (resourceName: string) => void;
    onRemove: (resourceName: string) => void;
}

export function ApiProductResourceRowActions({
    resource,
    enabled,
    isPending,
    onEdit,
    onToggle,
    onRemove,
}: ApiProductResourceRowActionsProps) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-8"
                    aria-label={`Actions for ${resource.name}`}
                >
                    <MoreHorizontalIcon className="size-4" />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    data-testid="api_product_resources_edit_button"
                    onSelect={() => onEdit(resource.name)}
                >
                    <PencilIcon className="size-4" />
                    Edit
                </DropdownMenuItem>
                <DropdownMenuItem
                    data-testid="api_product_resources_toggle_button"
                    onSelect={() => onToggle(resource.name)}
                    disabled={isPending}
                >
                    {enabled ? (
                        <>
                            <CircleXIcon className="size-4" />
                            Disable
                        </>
                    ) : (
                        <>
                            <CircleCheckIcon className="size-4" />
                            Enable
                        </>
                    )}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                    className="text-destructive focus:text-destructive"
                    data-testid="api_product_resources_remove_button"
                    onSelect={() => onRemove(resource.name)}
                    disabled={isPending}
                >
                    <Trash2Icon className="size-4" />
                    Remove
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
