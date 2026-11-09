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

import { Button, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

export function NewPortalButton() {
    const descriptionId = useId();
    return (
        <TooltipProvider>
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        type="button"
                        aria-disabled="true"
                        aria-describedby={descriptionId}
                        className="aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
                    >
                        <PlusIcon aria-hidden="true" />
                        New Portal
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Creating portals is coming soon.</TooltipContent>
            </Tooltip>
            <span id={descriptionId} className="sr-only">
                Creating portals is coming soon.
            </span>
        </TooltipProvider>
    );
}
