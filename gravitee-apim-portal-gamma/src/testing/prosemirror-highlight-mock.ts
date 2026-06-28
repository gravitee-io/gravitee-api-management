export const Plugin = class {};
export const PluginKey = class {
    constructor(public name: string) {}
};

export function createHighlightPlugin() {
    return new Plugin();
}
