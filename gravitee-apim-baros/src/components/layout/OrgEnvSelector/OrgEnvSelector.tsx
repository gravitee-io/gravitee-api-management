import { forwardRef, type ComponentPropsWithRef, type ElementType } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import { Button } from '@baros/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';

interface OrgOption {
  readonly key: string;
  readonly name: string;
  readonly icon?: ElementType;
}

interface EnvOption {
  readonly key: string;
  readonly name: string;
}

interface OrgEnvSelectorProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Available organizations. */
  readonly organizations: OrgOption[];
  /** Available environments. */
  readonly environments: EnvOption[];
  /** Key of the currently selected organization. */
  readonly activeOrgKey: string;
  /** Key of the currently selected environment. */
  readonly activeEnvKey: string;
  /** Callback when the organization selection changes. */
  readonly onOrgChange?: (key: string) => void;
  /** Callback when the environment selection changes. */
  readonly onEnvChange?: (key: string) => void;
}

const OrgEnvSelector = forwardRef<HTMLDivElement, OrgEnvSelectorProps>(
  (
    {
      organizations,
      environments,
      activeOrgKey,
      activeEnvKey,
      onOrgChange,
      onEnvChange,
      className,
      ...props
    },
    ref,
  ) => {
    const activeOrg = organizations.find(o => o.key === activeOrgKey);
    const activeEnv = environments.find(e => e.key === activeEnvKey);

    return (
      <div ref={ref} className={cn('flex items-center', className)} {...props}>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              className="h-7 gap-1.5 rounded-full px-3 text-xs font-medium"
            >
              {activeOrg?.icon && <activeOrg.icon className="size-3.5 shrink-0" />}
              <span className="truncate">{activeOrg?.name ?? 'Select org'}</span>
              <span className="text-muted-foreground">/</span>
              <span className="truncate">{activeEnv?.name ?? 'Select env'}</span>
              <ChevronDown className="size-3 shrink-0 opacity-50" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="min-w-52">
            <DropdownMenuGroup>
              <DropdownMenuLabel className="text-xs text-muted-foreground">
                Organization
              </DropdownMenuLabel>
              {organizations.map(org => (
                <DropdownMenuItem
                  key={org.key}
                  className="gap-2"
                  onSelect={() => onOrgChange?.(org.key)}
                >
                  {org.icon && <org.icon className="size-3.5 shrink-0" />}
                  <span className="flex-1">{org.name}</span>
                  {org.key === activeOrgKey && <Check className="size-3.5 shrink-0" />}
                </DropdownMenuItem>
              ))}
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuLabel className="text-xs text-muted-foreground">
                Environment
              </DropdownMenuLabel>
              {environments.map(env => (
                <DropdownMenuItem
                  key={env.key}
                  className="gap-2"
                  onSelect={() => onEnvChange?.(env.key)}
                >
                  <span className="flex-1">{env.name}</span>
                  {env.key === activeEnvKey && <Check className="size-3.5 shrink-0" />}
                </DropdownMenuItem>
              ))}
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    );
  },
);

OrgEnvSelector.displayName = 'OrgEnvSelector';

export { OrgEnvSelector };
export type { OrgEnvSelectorProps, OrgOption, EnvOption };
