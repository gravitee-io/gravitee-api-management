import { createApplication } from '@angular/platform-browser';
import { appConfig } from '../app.config';
import { RemoteEntry } from './entry';
import { ApplicationRef } from '@angular/core';

export async function mount(hostElement: HTMLElement): Promise<() => void> {
    const appRef: ApplicationRef = await createApplication(appConfig);
    const compRef = appRef.bootstrap(RemoteEntry, hostElement);

    return () => {
        compRef.destroy();
        appRef.destroy();
    };
}
