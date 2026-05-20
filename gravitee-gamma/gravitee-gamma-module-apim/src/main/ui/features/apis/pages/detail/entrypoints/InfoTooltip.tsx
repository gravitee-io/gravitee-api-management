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
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';

export function InfoTooltip({ text }: { readonly text: string }) {
    return (
        <TooltipProvider>
            <Tooltip>
                <TooltipTrigger asChild>
                    <button
                        type="button"
                        className="ml-1 inline-flex items-center align-middle text-muted-foreground hover:text-foreground"
                    >
                        <InfoIcon className="size-3.5" aria-hidden />
                        <span className="sr-only">More information</span>
                    </button>
                </TooltipTrigger>
                <TooltipContent side="right" className="max-w-xs">
                    <p className="text-xs leading-normal">{text}</p>
                </TooltipContent>
            </Tooltip>
        </TooltipProvider>
    );
}
