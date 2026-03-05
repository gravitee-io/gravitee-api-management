declare module 'app_alpha/Module';
declare module 'app_beta/Module' {
    export function mount(hostElement: HTMLElement): Promise<() => void>;
}
