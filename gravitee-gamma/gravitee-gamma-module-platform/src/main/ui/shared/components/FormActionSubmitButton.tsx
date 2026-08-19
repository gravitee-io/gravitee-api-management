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

import { Button } from '@gravitee/graphene-core';
import { useFormStatus } from 'react-dom';

export function FormActionSubmitButton({
    disabled,
    label,
    pendingLabel,
}: Readonly<{
    disabled: boolean;
    label: string;
    pendingLabel: string;
}>) {
    const { pending } = useFormStatus();

    return (
        <Button type="submit" disabled={disabled || pending}>
            {pending ? pendingLabel : label}
        </Button>
    );
}
