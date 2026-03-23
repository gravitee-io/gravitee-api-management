import { forwardRef, type ComponentPropsWithRef } from 'react';
import { cn } from '@baros/lib/utils';
import { ChevronDown } from 'lucide-react';

const Select = forwardRef<HTMLSelectElement, ComponentPropsWithRef<'select'>>(
  ({ className, children, ...props }, ref) => {
    return (
      <div className="relative">
        <select
          className={cn(
            'flex h-9 w-full appearance-none rounded-md border border-input bg-transparent px-3 py-1 pr-8 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50',
            className,
          )}
          ref={ref}
          {...props}
        >
          {children}
        </select>
        <ChevronDown className="absolute right-2 top-2.5 h-4 w-4 opacity-50 pointer-events-none" />
      </div>
    );
  },
);

Select.displayName = 'Select';

export { Select };
