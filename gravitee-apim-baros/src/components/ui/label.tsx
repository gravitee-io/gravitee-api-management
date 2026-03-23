import { forwardRef, type ComponentPropsWithRef } from 'react';
import { cn } from '@baros/lib/utils';

const Label = forwardRef<HTMLLabelElement, ComponentPropsWithRef<'label'>>(
  ({ className, ...props }, ref) => (
    <label
      ref={ref}
      className={cn(
        'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
        className,
      )}
      {...props}
    />
  ),
);

Label.displayName = 'Label';

export { Label };
