/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Where Graphene's ThemeProvider keeps the theme the user picked. Graphene 3.8 keeps the key private, and this file
 * could not import it anyway: it belongs to the console's entry, which must work when the libraries shared through
 * Module Federation, Graphene among them, failed to load. A test checks the value against Graphene.
 */
const THEME_STORAGE_KEY = 'graphene.theme.mode';

function prefersDarkTheme(): boolean {
    try {
        const mode = window.localStorage.getItem(THEME_STORAGE_KEY);
        if (mode === 'dark' || mode === 'light') return mode === 'dark';
    } catch {
        // Storage can be blocked: the system preference still applies.
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function element<K extends keyof HTMLElementTagNameMap>(tag: K, className: string, text?: string): HTMLElementTagNameMap[K] {
    const node = document.createElement(tag);
    node.className = className;
    if (text) node.textContent = text;
    return node;
}

const SVG_NAMESPACE = 'http://www.w3.org/2000/svg';

function svgElement(tag: string, attributes: Record<string, string>): SVGElement {
    const node = document.createElementNS(SVG_NAMESPACE, tag);
    Object.entries(attributes).forEach(([name, value]) => node.setAttribute(name, value));
    return node;
}

/** Lucide's CircleAlert, the icon of the message shown when a module is not available. */
function alertIcon(): SVGElement {
    const icon = svgElement('svg', {
        width: '16',
        height: '16',
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'aria-hidden': 'true',
    });
    icon.append(
        svgElement('circle', { cx: '12', cy: '12', r: '10' }),
        svgElement('line', { x1: '12', x2: '12', y1: '8', y2: '12' }),
        svgElement('line', { x1: '12', x2: '12.01', y1: '16', y2: '16' }),
    );
    return icon;
}

/**
 * Plain DOM rather than React, which may be what failed to load. The classes are those of Graphene's
 * Empty and Button, so the page matches the messages shown inside the console, and the title takes the
 * style of the console's page titles.
 *
 * The page needs no file of its own: during a rolling update, any other file can fail like the one that
 * brought the user here. The icon is drawn inline and there is no logo, as Graphene's Logo is an image
 * file. Inline styles keep the page centered and readable if the console's stylesheet failed too.
 */
function startupErrorPage(): HTMLElement {
    const reload = element(
        'button',
        'inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-transparent bg-primary bg-clip-padding px-2.5 text-sm font-medium whitespace-nowrap text-primary-foreground transition-all outline-none select-none hover:bg-primary/80 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50',
        'Reload page',
    );
    reload.type = 'button';
    reload.addEventListener('click', () => window.location.reload());

    // Red, which Graphene keeps for errors, as on the message shown when a module is not available.
    const media = element('div', 'mb-2 flex size-8 shrink-0 items-center justify-center rounded-lg bg-destructive/10 text-destructive');
    media.append(alertIcon());

    const header = element('div', 'flex max-w-sm flex-col items-center gap-2');
    header.append(
        media,
        element('h1', 'text-2xl font-bold tracking-tight', "The console couldn't start"),
        element(
            'p',
            'text-sm/relaxed text-muted-foreground',
            // The no-break space keeps "If this" together, so the line breaks between the two sentences.
            'It may be updating. Reload the page in a minute. If\u00a0this keeps happening, contact your administrator.',
        ),
    );

    const actions = element('div', 'flex w-full max-w-sm min-w-0 flex-col items-center gap-2.5 text-sm text-balance');
    actions.append(reload);

    const message = element(
        'div',
        'flex w-full min-w-0 flex-1 flex-col items-center justify-center gap-4 rounded-xl p-6 text-center text-balance',
    );
    message.setAttribute('role', 'alert');
    message.append(header, actions);

    const page = element('main', 'flex min-h-svh items-center justify-center');
    Object.assign(page.style, {
        display: 'flex',
        minHeight: '100svh',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        fontFamily: 'var(--font-sans, system-ui, sans-serif)',
    });
    page.append(message);
    return page;
}

export function showStartupError(error: unknown): void {
    console.error('[Startup] The console could not start.', error);
    document.documentElement.classList.toggle('dark', prefersDarkTheme());
    (document.getElementById('root') ?? document.body).replaceChildren(startupErrorPage());
}
