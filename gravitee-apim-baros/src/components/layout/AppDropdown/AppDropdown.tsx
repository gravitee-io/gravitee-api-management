import { forwardRef, type ComponentPropsWithRef, type ElementType } from 'react';
import { Check, ChevronsUpDown } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';

interface AppOption {
  /** Unique key for the app. */
  readonly key: string;
  /** Display name (e.g. "API Management"). */
  readonly name: string;
  /** Secondary label shown below the name. */
  readonly description?: string;
  /** Icon component rendered before the name. */
  readonly icon?: ElementType;
}

interface AppDropdownProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Available applications. */
  readonly apps: AppOption[];
  /** Key of the currently active application. */
  readonly activeAppKey: string;
  /** Callback when the application selection changes. */
  readonly onAppChange?: (key: string) => void;
}

const AppDropdown = forwardRef<HTMLDivElement, AppDropdownProps>(
  ({ apps, activeAppKey, onAppChange, className, ...props }, ref) => {
    const activeApp = apps.find((a) => a.key === activeAppKey);

    return (
      <div ref={ref} className={cn('flex items-center', className)} {...props}>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex items-center gap-1.5 rounded-md px-2 py-1 text-left hover:bg-accent"
            >
              {activeApp?.icon && <activeApp.icon className="size-4 shrink-0" />}
              <span className="truncate text-sm font-medium">{activeApp?.name ?? 'Select app'}</span>
              <ChevronsUpDown className="size-3.5 shrink-0 opacity-50" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="min-w-48">
            {apps.map((app) => (
              <DropdownMenuItem key={app.key} className="gap-2" onSelect={() => onAppChange?.(app.key)}>
                {app.icon && <app.icon className="size-4 shrink-0" />}
                <span className="flex-1 truncate">{app.name}</span>
                {app.key === activeAppKey && <Check className="size-3.5 shrink-0" />}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    );
  },
);

AppDropdown.displayName = 'AppDropdown';

export { AppDropdown };
export type { AppDropdownProps, AppOption };
