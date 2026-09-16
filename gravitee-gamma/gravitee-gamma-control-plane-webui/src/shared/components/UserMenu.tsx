/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Avatar,
    AvatarFallback,
    AvatarImage,
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    type ThemeMode,
    useTheme,
} from '@gravitee/graphene-core';

const THEME_MODES: ReadonlyArray<{ readonly value: ThemeMode; readonly label: string }> = [
    { value: 'light', label: 'Light' },
    { value: 'dark', label: 'Dark' },
    { value: 'system', label: 'System' },
];

function initialsFromName(name: string): string {
    return (
        name
            .split(/\s+/)
            .map(part => part[0] ?? '')
            .join('')
            .toUpperCase()
            .slice(0, 2) || '?'
    );
}

export function UserMenu({
    name,
    email,
    avatarSrc,
    onMyAccount,
    onSignOut,
}: Readonly<{
    name: string;
    email?: string;
    avatarSrc?: string;
    onMyAccount: () => void;
    onSignOut: () => void;
}>) {
    const { mode, setMode } = useTheme();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button type="button" variant="ghost" size="icon-sm" className="rounded-full" aria-label="Account menu">
                    <Avatar size="sm">
                        {avatarSrc ? <AvatarImage src={avatarSrc} alt="" /> : null}
                        <AvatarFallback className="bg-primary/10 text-primary text-xs font-semibold">
                            {initialsFromName(name)}
                        </AvatarFallback>
                    </Avatar>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
                align="end"
                // Graphene sets width to the trigger (`w-(--radix-dropdown-menu-trigger-width)`).
                // The avatar button is ~32px, so without an override the labels wrap to two letters.
                className="w-auto min-w-56"
                style={{ width: 'max-content', minWidth: '14rem' }}
            >
                <DropdownMenuLabel className="space-y-0.5">
                    <p className="truncate text-sm font-medium text-foreground">{name}</p>
                    {email ? <p className="truncate text-xs font-normal text-muted-foreground">{email}</p> : null}
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="whitespace-nowrap" onSelect={() => onMyAccount()}>
                    My Account
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">Theme</DropdownMenuLabel>
                <DropdownMenuRadioGroup value={mode} onValueChange={value => setMode(value as ThemeMode)}>
                    {THEME_MODES.map(theme => (
                        <DropdownMenuRadioItem key={theme.value} className="whitespace-nowrap" value={theme.value}>
                            {theme.label}
                        </DropdownMenuRadioItem>
                    ))}
                </DropdownMenuRadioGroup>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="whitespace-nowrap" onSelect={() => onSignOut()}>
                    Sign out
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
