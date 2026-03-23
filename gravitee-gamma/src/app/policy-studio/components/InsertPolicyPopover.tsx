import { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Plus } from 'lucide-react';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { cn } from '@baros/lib/utils';
import type { PolicyPlugin } from '../types';

interface InsertPolicyPopoverProps {
  readonly policies: PolicyPlugin[];
  readonly onSelect: (policyId: string) => void;
}

export function InsertPolicyPopover({ policies, onSelect }: InsertPolicyPopoverProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const triggerRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const filtered = policies.filter(
    (p) => p.name.toLowerCase().includes(search.toLowerCase()) || p.id.toLowerCase().includes(search.toLowerCase()),
  );

  const updatePosition = useCallback(() => {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    setPosition({ top: rect.bottom + 4, left: rect.left });
  }, []);

  useEffect(() => {
    if (!open) return;
    updatePosition();
    inputRef.current?.focus();

    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      if (
        triggerRef.current && !triggerRef.current.contains(target) &&
        dropdownRef.current && !dropdownRef.current.contains(target)
      ) {
        setOpen(false);
        setSearch('');
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open, updatePosition]);

  function handleSelect(policyId: string) {
    onSelect(policyId);
    setOpen(false);
    setSearch('');
  }

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen(!open)}
        className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-dashed border-muted-foreground/30 opacity-0 transition-opacity hover:border-primary hover:text-primary group-hover/phase:opacity-100"
        aria-label="Insert policy here"
      >
        <Plus className="h-3 w-3" />
      </button>
      {open && createPortal(
        <div
          ref={dropdownRef}
          className="fixed z-50 w-56 rounded-md border bg-popover p-2 shadow-md"
          style={{ top: position.top, left: position.left }}
        >
          <input
            ref={inputRef}
            placeholder="Search policy..."
            value={search}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
            className={cn(
              'mb-2 flex h-7 w-full rounded-md border border-input bg-transparent px-2 text-xs',
              'placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
            )}
          />
          <ScrollArea className="max-h-48">
            {filtered.map((policy) => (
              <button
                key={policy.id}
                type="button"
                className="flex w-full items-center rounded px-2 py-1.5 text-xs hover:bg-accent"
                onClick={() => handleSelect(policy.id)}
              >
                {policy.name || policy.id}
              </button>
            ))}
            {filtered.length === 0 && (
              <p className="px-2 py-1.5 text-xs text-muted-foreground">No compatible policy</p>
            )}
          </ScrollArea>
        </div>,
        document.body,
      )}
    </>
  );
}
