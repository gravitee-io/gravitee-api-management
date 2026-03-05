import { useEffect, useRef } from 'react';

const REMOTE_ENTRY_URL = 'http://localhost:4202/remoteEntry.mjs';

export function AngularWrapper() {
    const mountPointRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let destroyAngularApp: (() => void) | undefined;

        const loadAngular = async () => {
            if (!mountPointRef.current) return;

            try {
                await import('zone.js');

                const container = await import(
                    /* webpackIgnore: true */ REMOTE_ENTRY_URL
                );

                await __webpack_init_sharing__('default');

                await container.init(__webpack_share_scopes__.default);

                const factory = await container.get('./Module');
                const { mount } = factory();

                destroyAngularApp = await mount(mountPointRef.current, { basePath: '/app-beta' });
            } catch (error) {
                console.error('Failed to load Angular remote:', error);
            }
        };

        loadAngular();

        return () => {
            destroyAngularApp?.();
        };
    }, []);

    return <div ref={mountPointRef} className="angular-micro-frontend-container h-full w-full" />;
}
