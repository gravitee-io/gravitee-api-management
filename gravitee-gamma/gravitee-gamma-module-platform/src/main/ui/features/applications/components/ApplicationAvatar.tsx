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
import { Avatar, AvatarFallback, AvatarImage } from '@gravitee/graphene-core';
import { AppWindowIcon } from '@gravitee/graphene-core/icons';

interface ApplicationAvatarProps {
    readonly src?: string;
    readonly name: string;
}

/** Application logo with a graceful fallback to the application glyph (mirrors APIM `ApiAvatar`). */
export function ApplicationAvatar({ src, name }: ApplicationAvatarProps) {
    return (
        <Avatar size="sm" className="shrink-0 rounded-md">
            {src ? <AvatarImage src={src} alt={`${name} logo`} className="rounded-md object-cover" /> : null}
            <AvatarFallback className="rounded-md bg-primary/10 text-primary">
                <AppWindowIcon className="size-3.5" aria-hidden />
            </AvatarFallback>
        </Avatar>
    );
}
