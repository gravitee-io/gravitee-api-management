/**
 * Inline Entity ID display: dotted id with a ghost-icon copy button.
 * Colocated with the policy-structure feature (only consumers are EntitiesPage
 * and EntityDetailSheet) — composed entirely from Graphene primitives.
 */
import { Button } from '@gravitee/graphene-core';
import { Copy } from 'lucide-react';

interface EntityIdChipProps {
    value: string;
    maxWidthClassName?: string;
}

export function EntityIdChip({ value, maxWidthClassName = 'max-w-[220px]' }: EntityIdChipProps) {
    const copyValue = async () => {
        try {
            await navigator.clipboard.writeText(value);
        } catch {
            // Clipboard not available in all environments
        }
    };

    const segments = value.split('.');
    const breakIndex = Math.ceil(segments.length / 2);
    const firstLine = segments.slice(0, breakIndex).join('.');
    const secondLine = segments.slice(breakIndex).join('.');

    return (
        <div className="flex min-w-0 items-center gap-1">
            <code className={`block ${maxWidthClassName} overflow-hidden font-mono text-xs text-muted-foreground`}>
                <span className="block truncate">{firstLine}</span>
                {secondLine ? <span className="block truncate">{secondLine}</span> : null}
            </code>
            <Button
                type="button"
                variant="ghost"
                size="icon"
                className="size-6 shrink-0 self-center text-muted-foreground hover:text-foreground"
                onClick={event => {
                    event.stopPropagation();
                    void copyValue();
                }}
                aria-label="Copy entity ID"
            >
                <Copy className="size-3" />
            </Button>
        </div>
    );
}
