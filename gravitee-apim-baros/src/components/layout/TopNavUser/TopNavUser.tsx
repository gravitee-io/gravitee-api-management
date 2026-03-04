import { forwardRef, type ComponentPropsWithRef } from 'react';
import { cn } from '@baros/lib/utils';
import { Avatar, AvatarFallback, AvatarImage } from '@baros/components/ui/avatar';
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

interface UserInfo {
  readonly name: string;
  readonly email: string;
  readonly avatar?: string;
}

interface TopNavUserProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** User information displayed in the trigger and dropdown header. */
  readonly user: UserInfo;
  /** Callback fired when a menu action is selected. */
  readonly onAction?: (action: string) => void;
}

const TopNavUser = forwardRef<HTMLDivElement, TopNavUserProps>(
  ({ user, onAction, className, ...props }, ref) => {
    const initials = user.name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);

    return (
      <div ref={ref} className={cn('flex items-center', className)} {...props}>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="relative h-8 w-8 rounded-full">
              <Avatar className="h-8 w-8">
                {user.avatar && <AvatarImage src={user.avatar} alt={user.name} />}
                <AvatarFallback className="text-xs">{initials}</AvatarFallback>
              </Avatar>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" sideOffset={8} className="min-w-56">
            <DropdownMenuLabel className="p-0 font-normal">
              <div className="flex items-center gap-2 px-2 py-1.5 text-left text-sm">
                <Avatar className="h-8 w-8">
                  {user.avatar && <AvatarImage src={user.avatar} alt={user.name} />}
                  <AvatarFallback className="text-xs">{initials}</AvatarFallback>
                </Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">{user.name}</span>
                  <span className="truncate text-xs text-muted-foreground">{user.email}</span>
                </div>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem onSelect={() => onAction?.('account')}>Account</DropdownMenuItem>
              <DropdownMenuItem onSelect={() => onAction?.('settings')}>Settings</DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem onSelect={() => onAction?.('logout')}>Log out</DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    );
  },
);

TopNavUser.displayName = 'TopNavUser';

export { TopNavUser };
export type { TopNavUserProps, UserInfo };
