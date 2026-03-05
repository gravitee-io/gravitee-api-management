import { useEffect, useRef } from 'react';

export function AngularWrapper() {
    const containerRef = useRef<HTMLDivElement>(null);
    const unmountRef = useRef<(() => void) | null>(null);

    useEffect(() => {
        const container = containerRef.current;
        if (!container) return;

        import('app_beta/Module').then(({ mount }) => {
            mount(container).then((unmount: () => void) => {
                unmountRef.current = unmount;
            });
        });

        return () => {
            unmountRef.current?.();
            unmountRef.current = null;
        };
    }, []);

    return <div ref={containerRef} />;
}
