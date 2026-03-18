import { useState } from 'react';
import { Search } from 'lucide-react';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { Separator } from '@baros/components/ui/separator';
import type { PolicyPlugin } from '../types';
import { PolicyCatalogItem } from './PolicyCatalogItem';

interface PolicyCatalogProps {
  readonly policies: PolicyPlugin[];
}

export function PolicyCatalog({ policies }: PolicyCatalogProps) {
  const [search, setSearch] = useState('');

  const filtered = policies.filter((p) => {
    const name = p.name ?? p.id ?? '';
    return name.toLowerCase().includes(search.toLowerCase());
  });

  return (
    <div className="flex w-64 shrink-0 flex-col border-l">
      <div className="px-3 py-2">
        <h2 className="text-sm font-semibold">Policies</h2>
      </div>
      <Separator />
      <div className="relative px-2 py-2">
        <Search className="absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Filter..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-8 w-full rounded-md border border-input bg-background pl-8 pr-2 text-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          aria-label="Filter policies"
        />
      </div>
      <ScrollArea className="flex-1">
        <div className="flex flex-col gap-1 p-2">
          {filtered.length === 0 && (
            <div className="px-2 py-8 text-center text-xs text-muted-foreground">
              No policies found
            </div>
          )}
          {filtered.map((policy) => (
            <PolicyCatalogItem key={policy.id} policy={policy} />
          ))}
        </div>
      </ScrollArea>
    </div>
  );
}
