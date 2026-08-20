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
import { Alert, AlertDescription } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';

import { MemberSuccessorCombobox } from './MemberSuccessorCombobox';
import type { GroupMember } from '../types/group';

type MemberOwnershipTransferFieldProps = Readonly<{
    id: string;
    candidates: GroupMember[];
    value: GroupMember | null;
    onChange: (member: GroupMember | null) => void;
    message: string | null;
    disabled: boolean;
}>;

export function MemberOwnershipTransferField({ id, candidates, value, onChange, message, disabled }: MemberOwnershipTransferFieldProps) {
    return (
        <div className="space-y-3">
            <MemberSuccessorCombobox
                id={id}
                candidates={candidates}
                value={value}
                onChange={onChange}
                hint="Select a member to transfer primary ownership."
                disabled={disabled}
            />
            {message ? (
                <Alert variant="default">
                    <InfoIcon className="size-4" aria-hidden />
                    <AlertDescription>{message}</AlertDescription>
                </Alert>
            ) : null}
        </div>
    );
}
