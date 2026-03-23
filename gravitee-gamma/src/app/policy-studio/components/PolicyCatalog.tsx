import { useState, useMemo } from 'react';
import { Search, ChevronDown } from 'lucide-react';
import { Collapsible, CollapsibleTrigger, CollapsibleContent } from '@baros/components/ui/collapsible';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { Separator } from '@baros/components/ui/separator';
import { cn } from '@baros/lib/utils';
import type { PolicyPlugin } from '../types';
import { PolicyCatalogItem } from './PolicyCatalogItem';

const UNCATEGORIZED = 'Others';

interface PolicyCatalogProps {
  readonly policies: PolicyPlugin[];
}

export function PolicyCatalog({ policies }: PolicyCatalogProps) {
  const [search, setSearch] = useState('');

  const filtered = useMemo(
    () => policies.filter((p) => {
      const name = p.name ?? p.id ?? '';
      return name.toLowerCase().includes(search.toLowerCase());
    }),
    [policies, search],
  );

  const grouped = useMemo(() => {
    const map = new Map<string, PolicyPlugin[]>();
    for (const p of filtered) {
      const cat = p.category || UNCATEGORIZED;
      const list = map.get(cat);
      if (list) list.push(p);
      else map.set(cat, [p]);
    }
    return [...map.entries()].sort(([a], [b]) => {
      if (a === UNCATEGORIZED) return 1;
      if (b === UNCATEGORIZED) return -1;
      return a.localeCompare(b);
    });
  }, [filtered]);

  const isSearching = search.length > 0;

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
        <div className="flex flex-col p-2">
          {filtered.length === 0 && (
            <div className="px-2 py-8 text-center text-xs text-muted-foreground">
              No policies found
            </div>
          )}
          {grouped.map(([category, items]) => (
            <Collapsible key={category} defaultOpen open={isSearching ? true : undefined} className="group/collapsible">
              <CollapsibleTrigger className="flex w-full items-center gap-1 px-2 py-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground">
                <ChevronDown className={cn(
                  'h-3 w-3 transition-transform',
                  'group-data-[state=closed]/collapsible:-rotate-90',
                )} />
                {category}
                <span className="ml-auto text-[10px] font-normal">{items.length}</span>
              </CollapsibleTrigger>
              <CollapsibleContent>
                <div className="flex flex-col gap-1 pb-1">
                  {items.map((policy) => (
                    <PolicyCatalogItem key={policy.id} policy={policy} />
                  ))}
                </div>
              </CollapsibleContent>
            </Collapsible>
          ))}
        </div>
      </ScrollArea>
    </div>
  );
}
