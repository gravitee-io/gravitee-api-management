import { forwardRef, type ComponentPropsWithRef, type ElementType } from 'react';
import { Check, ChevronsUpDown } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';

/* ──────────────────────────────────────────────────── */
/*  Types                                               */
/* ──────────────────────────────────────────────────── */

interface OrgOption {
  readonly key: string;
  readonly name: string;
  /** Secondary label shown below the name (e.g. plan tier). */
  readonly description?: string;
  readonly icon?: ElementType;
}

interface EnvOption {
  readonly key: string;
  readonly name: string;
}

/* ──────────────────────────────────────────────────── */
/*  OrgSelector                                         */
/* ──────────────────────────────────────────────────── */

interface OrgSelectorProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Available organizations. */
  readonly organizations: OrgOption[];
  /** Key of the currently selected organization. */
  readonly activeOrgKey: string;
  /** Callback when the organization selection changes. */
  readonly onOrgChange?: (key: string) => void;
}

const OrgSelector = forwardRef<HTMLDivElement, OrgSelectorProps>(
  ({ organizations, activeOrgKey, onOrgChange, className, ...props }, ref) => {
    const activeOrg = organizations.find((o) => o.key === activeOrgKey);

    return (
      <div ref={ref} className={cn('w-full', className)} {...props}>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex w-full items-center gap-2 rounded-md p-2 text-left text-sidebar-foreground hover:bg-sidebar-accent/50"
            >
              {activeOrg?.icon && (
                <activeOrg.icon className="size-4 shrink-0" />
              )}
              <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-semibold">{activeOrg?.name ?? 'Select org'}</div>
                {activeOrg?.description && (
                  <div className="truncate text-xs text-muted-foreground">{activeOrg.description}</div>
                )}
              </div>
              <ChevronsUpDown className="size-4 shrink-0 opacity-50" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="w-[var(--radix-dropdown-menu-trigger-width)]">
            {organizations.map((org) => (
              <DropdownMenuItem key={org.key} className="gap-2" onSelect={() => onOrgChange?.(org.key)}>
                {org.icon && <org.icon className="size-4 shrink-0" />}
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm">{org.name}</div>
                  {org.description && <div className="truncate text-xs text-muted-foreground">{org.description}</div>}
                </div>
                {org.key === activeOrgKey && <Check className="size-3.5 shrink-0" />}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    );
  },
);

OrgSelector.displayName = 'OrgSelector';

/* ──────────────────────────────────────────────────── */
/*  EnvSelector                                         */
/* ──────────────────────────────────────────────────── */

interface EnvSelectorProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Available environments. */
  readonly environments: EnvOption[];
  /** Key of the currently selected environment. */
  readonly activeEnvKey: string;
  /** Callback when the environment selection changes. */
  readonly onEnvChange?: (key: string) => void;
}

const EnvSelector = forwardRef<HTMLDivElement, EnvSelectorProps>(
  ({ environments, activeEnvKey, onEnvChange, className, ...props }, ref) => {
    const activeEnv = environments.find((e) => e.key === activeEnvKey);

    return (
      <div ref={ref} className={cn('w-full', className)} {...props}>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex w-full items-center justify-between gap-1.5 rounded-md border border-sidebar-border px-3 py-1.5 text-xs font-medium text-sidebar-foreground hover:bg-sidebar-accent/50"
            >
              <span className="truncate">{activeEnv?.name ?? 'Select env'}</span>
              <ChevronsUpDown className="size-3 shrink-0 opacity-50" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            align="start"
            className="w-[var(--radix-dropdown-menu-trigger-width)]"
          >
            {environments.map((env) => (
              <DropdownMenuItem key={env.key} className="gap-2" onSelect={() => onEnvChange?.(env.key)}>
                <span className="flex-1">{env.name}</span>
                {env.key === activeEnvKey && <Check className="size-3.5 shrink-0" />}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    );
  },
);

EnvSelector.displayName = 'EnvSelector';

export { OrgSelector, EnvSelector };
export type { OrgSelectorProps, EnvSelectorProps, OrgOption, EnvOption };
