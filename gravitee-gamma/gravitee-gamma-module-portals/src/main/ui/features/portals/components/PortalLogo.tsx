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

interface PortalLogoProps {
    readonly name: string;
    readonly src: string | null;
}

export function PortalLogo({ name, src }: PortalLogoProps) {
    const initials = name
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(word => word.charAt(0))
        .join('')
        .toUpperCase();

    return (
        <Avatar className="shrink-0 rounded-md">
            {src ? <AvatarImage src={src} alt={`${name} logo`} className="rounded-md object-cover" /> : null}
            <AvatarFallback className="rounded-md bg-muted text-foreground" aria-hidden="true">
                {initials}
            </AvatarFallback>
        </Avatar>
    );
}
