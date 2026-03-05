import { createApplication } from '@angular/platform-browser';
import { appConfig } from '../app.config';
import { RemoteEntry } from './entry';
import { ApplicationRef, Provider } from '@angular/core';
import { APP_BASE_HREF } from '@angular/common';

interface MountOptions {
    basePath?: string;
}

export async function mount(hostElement: HTMLElement, options?: MountOptions): Promise<() => void> {
    const extraProviders: Provider[] = [];
    if (options?.basePath) {
        extraProviders.push({ provide: APP_BASE_HREF, useValue: options.basePath });
    }

    const appRef: ApplicationRef = await createApplication({
        ...appConfig,
        providers: [...appConfig.providers, ...extraProviders],
    });
    const compRef = appRef.bootstrap(RemoteEntry, hostElement);

    return () => {
        compRef.destroy();
        appRef.destroy();
    };
}
