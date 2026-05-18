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
