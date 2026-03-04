import { cn } from '@baros/lib/utils';
import graviteeLogoLight from '@baros/assets/gravitee-logo-light.png';
import graviteeLogoDark from '@baros/assets/gravitee-logo-dark.png';
import graviteeIcon from '@baros/assets/gravitee-icon.png';

interface GraviteeLogoProps {
  /** Additional CSS classes applied to the wrapper. */
  readonly className?: string;
}

/**
 * Full horizontal Gravitee logo that switches between light and dark variants
 * based on the active theme (`.dark` class).
 */
function GraviteeLogo({ className }: GraviteeLogoProps) {
  return (
    <span className={cn('inline-flex', className)}>
      <img src={graviteeLogoLight} alt="Gravitee" className="h-5 w-auto dark:hidden" />
      <img src={graviteeLogoDark} alt="Gravitee" className="hidden h-5 w-auto dark:block" />
    </span>
  );
}

interface GraviteeIconProps {
  /** Additional CSS classes applied to the `<img>`. */
  readonly className?: string;
}

/**
 * Compact Gravitee "G" icon used in the collapsed sidebar.
 * Theme-agnostic — the solaris icon works on any background.
 */
function GraviteeIcon({ className }: GraviteeIconProps) {
  return <img src={graviteeIcon} alt="Gravitee" className={cn('size-7', className)} />;
}

export { GraviteeLogo, GraviteeIcon };
export type { GraviteeLogoProps, GraviteeIconProps };
