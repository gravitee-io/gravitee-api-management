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
/**
 * Lightweight Tabs wrapper over @radix-ui/react-tabs,
 * matching the API used in PolicyEditorSheet (same surface as shadcn/ui Tabs).
 */
import { cn } from '@gravitee/graphene-core';
import * as RadixTabs from '@radix-ui/react-tabs';
import * as React from 'react';

export const Tabs = RadixTabs.Root;

export const TabsList = React.forwardRef<React.ElementRef<typeof RadixTabs.List>, React.ComponentPropsWithoutRef<typeof RadixTabs.List>>(
    ({ className, ...props }, ref) => (
        <RadixTabs.List
            ref={ref}
            className={cn('inline-flex items-center gap-1 border-b border-border', className)}
            {...props}
        />
    ),
);
TabsList.displayName = 'TabsList';

export const TabsTrigger = React.forwardRef<
    React.ElementRef<typeof RadixTabs.Trigger>,
    React.ComponentPropsWithoutRef<typeof RadixTabs.Trigger>
>(({ className, ...props }, ref) => (
    <RadixTabs.Trigger
        ref={ref}
        className={cn(
            'relative inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium',
            'text-muted-foreground transition-colors hover:text-foreground',
            'border-b-2 border-transparent -mb-px',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded-t-sm',
            'data-[state=active]:text-foreground data-[state=active]:border-primary',
            'disabled:pointer-events-none disabled:opacity-50',
            className,
        )}
        {...props}
    />
));
TabsTrigger.displayName = 'TabsTrigger';

export const TabsContent = RadixTabs.Content;
