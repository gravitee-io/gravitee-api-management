import { useEffect, useRef } from 'react';

const REMOTE_ENTRY_URL = 'http://localhost:4000/remoteEntry.mjs';

export function PortalHomepageRemote() {
    const mountRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let unmountFn: (() => void) | undefined;

        const loadRemote = async () => {
            if (!mountRef.current) return;

            try {
                // #region agent log
                fetch('http://127.0.0.1:7243/ingest/deaec3c8-039f-42ae-bc14-e332b5e7094e',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'269500'},body:JSON.stringify({sessionId:'269500',location:'PortalHomepageRemote.tsx:loadRemote:start',message:'loadRemote started',data:{hasMountRef:!!mountRef.current},timestamp:Date.now(),hypothesisId:'H1'})}).catch(()=>{});
                // #endregion
                await import('zone.js');

                const container = await import(
                    /* webpackIgnore: true */ REMOTE_ENTRY_URL
                );

                // #region agent log
                fetch('http://127.0.0.1:7243/ingest/deaec3c8-039f-42ae-bc14-e332b5e7094e',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'269500'},body:JSON.stringify({sessionId:'269500',location:'PortalHomepageRemote.tsx:afterContainerImport',message:'container imported',data:{hasInit:typeof container.init},timestamp:Date.now(),hypothesisId:'H3'})}).catch(()=>{});
                // #endregion
                await __webpack_init_sharing__('default');

                await container.init(__webpack_share_scopes__.default);

                // #region agent log
                fetch('http://127.0.0.1:7243/ingest/deaec3c8-039f-42ae-bc14-e332b5e7094e',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'269500'},body:JSON.stringify({sessionId:'269500',location:'PortalHomepageRemote.tsx:afterInit',message:'container.init done',data:{},timestamp:Date.now(),hypothesisId:'H3'})}).catch(()=>{});
                // #endregion
                const factory = await container.get('./PortalHomepage');
                const { mount } = factory();

                // #region agent log
                fetch('http://127.0.0.1:7243/ingest/deaec3c8-039f-42ae-bc14-e332b5e7094e',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'269500'},body:JSON.stringify({sessionId:'269500',location:'PortalHomepageRemote.tsx:beforeMount',message:'calling mount()',data:{},timestamp:Date.now(),hypothesisId:'H2'})}).catch(()=>{});
                // #endregion
                const result = await mount(mountRef.current, {
                    baseURL: 'http://localhost:4200/management',
                    basePath: '/developer-portal/homepage',
                });
                unmountFn = result?.unmount;
                // #region agent log
                fetch('http://127.0.0.1:7243/ingest/deaec3c8-039f-42ae-bc14-e332b5e7094e',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'269500'},body:JSON.stringify({sessionId:'269500',location:'PortalHomepageRemote.tsx:mountDone',message:'mount() resolved',data:{hasUnmount:!!unmountFn},timestamp:Date.now(),hypothesisId:'H2'})}).catch(()=>{});
                // #endregion
            } catch (error) {
                // #region agent log
                fetch('http://127.0.0.1:7243/ingest/deaec3c8-039f-42ae-bc14-e332b5e7094e',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'269500'},body:JSON.stringify({sessionId:'269500',location:'PortalHomepageRemote.tsx:catch',message:'loadRemote error',data:{name:(error as Error).name,message:(error as Error).message},timestamp:Date.now(),hypothesisId:'H1-H2'})}).catch(()=>{});
                // #endregion
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
