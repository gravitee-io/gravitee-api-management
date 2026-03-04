import { cn } from '@baros/lib/utils';
import graviteeIcon from '@baros/assets/gravitee-icon.png';

interface GraviteeLogoProps {
  /** Additional CSS classes applied to the `<img>`. */
  readonly className?: string;
}

function GraviteeLogo({ className }: GraviteeLogoProps) {
  return <img src={graviteeIcon} alt="Gravitee" className={cn('size-7', className)} />;
}

export { GraviteeLogo };
export type { GraviteeLogoProps };
