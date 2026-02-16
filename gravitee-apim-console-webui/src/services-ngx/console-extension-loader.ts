/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

export type ConsoleExtensionPlacement = 'overlay' | 'top' | 'bottom' | 'left' | 'center' | 'right';

export interface ConsoleExtensionComponent {
  tagName: string;
  placement: ConsoleExtensionPlacement;
  label?: string;
  icon?: string;
}

export interface ConsoleExtensionManifest {
  entrypoint: string;
  components: ConsoleExtensionComponent[];
}

export interface ConsoleExtension {
  id: string;
  name: string;
  version: string;
  manifest: ConsoleExtensionManifest | null;
}

const requestConfig: RequestInit = {
  headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' },
};

export async function loadConsoleExtensions(baseURL: string): Promise<void> {
  try {
    (window as any).__GRAVITEE_CONSOLE_BASE_URL__ = new URL(baseURL, window.location.origin).pathname;
    const response = await fetch(`${baseURL}/v2/extensions/`, requestConfig);
    if (!response.ok) {
      return;
    }

    const extensions: ConsoleExtension[] = await response.json();
    const uiExtensions = extensions.filter((e) => e.manifest != null);

    // Build per-extension asset base URLs before loading any scripts
    const extensionAssets: Record<string, string> = {};
    for (const ext of uiExtensions) {
      extensionAssets[ext.id] = `${baseURL}/v2/extensions/${ext.id}/assets`;
    }
    (window as any).__GRAVITEE_CONSOLE_EXTENSION_ASSETS__ = extensionAssets;

    const loadPromises = uiExtensions.map((ext) => {
      const scriptUrl = `${baseURL}/v2/extensions/${ext.id}/assets/${ext.manifest!.entrypoint}`;
      return new Promise<void>((resolve) => {
        const script = document.createElement('script');
        script.src = scriptUrl;
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => resolve();
        document.head.appendChild(script);
      });
    });

    await Promise.all(loadPromises);

    (window as any).__GRAVITEE_CONSOLE_EXTENSIONS__ = uiExtensions;
  } catch {
    // silently ignore — extensions are optional
  }
}
