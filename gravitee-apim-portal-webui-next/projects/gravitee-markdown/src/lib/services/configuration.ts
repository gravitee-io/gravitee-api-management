import { InjectionToken } from "@angular/core";

export const GRAVITEE_MARKDOWN_BASE_URL = new InjectionToken<string>('gravitee-markdown-base-url', {
  factory: () => {
    console.warn('GRAVITEE_MARKDOWN_BASE_URL not provided, using default. Please provide this token in your application configuration.');
    return 'http://localhost:4101/portal/environments/DEFAULT';
  },
});

export const GRAVITEE_MARKDOWN_MOCK_MODE = new InjectionToken<boolean>('gravitee-markdown-mock-mode', {
  factory: () => {
    console.warn('GRAVITEE_MARKDOWN_MOCK_MODE not provided, using default (false). Please provide this token in your application configuration.');
    return false;
  },
});
