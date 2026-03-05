import { InjectionToken } from '@angular/core';

export interface PortalConstants {
  env: {
    baseURL: string;
    v2BaseURL: string;
  };
  baseURL: string;
}

// eslint-disable-next-line no-redeclare
export const PortalConstants = new InjectionToken<PortalConstants>('PortalConstants');
