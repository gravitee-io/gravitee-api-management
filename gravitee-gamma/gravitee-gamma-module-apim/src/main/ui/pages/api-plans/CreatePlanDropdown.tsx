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
import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@gravitee/graphene-core';
import { ChevronDownIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import type { PlanContext, PlanSecurityType } from '../../features/apis/types/plan';
import { PLAN_SECURITY_LABELS, PLAN_TYPES_BY_CTX } from '../../features/apis/types/plan';

interface CreatePlanDropdownProps {
    ctx: PlanContext;
}

export function CreatePlanDropdown({ ctx }: Readonly<CreatePlanDropdownProps>) {
    const navigate = useNavigate();
    const types: PlanSecurityType[] = PLAN_TYPES_BY_CTX[ctx.type];

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button type="button" size="sm">
                    <PlusIcon className="size-4" aria-hidden />
                    Create plan
                    <ChevronDownIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                {types.map(type => (
                    <DropdownMenuItem key={type} onClick={() => navigate(`new/${type}`)}>
                        {PLAN_SECURITY_LABELS[type]}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
