import { useEffect, useRef } from 'react';

const REMOTE_ENTRY_URL = 'http://localhost:4000/remoteEntry.mjs';

export function PortalHomepageRemote() {
    const mountRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let unmountFn: (() => void) | undefined;

        const loadRemote = async () => {
            if (!mountRef.current) return;

            try {
                await import('zone.js');

                const container = await import(
                    /* webpackIgnore: true */ REMOTE_ENTRY_URL
                );

                await __webpack_init_sharing__('default');

                await container.init(__webpack_share_scopes__.default);

                const factory = await container.get('./PortalHomepage');
                const { mount } = factory();

                const result = await mount(mountRef.current);
                unmountFn = result?.unmount;
            } catch (error) {
                console.error('Failed to load console-webui PortalHomepage remote:', error);
            }
        };

        loadRemote();

        return () => {
            unmountFn?.();
        };
    }, []);

    return <div ref={mountRef} className="flex-1 p-6" />;
}
