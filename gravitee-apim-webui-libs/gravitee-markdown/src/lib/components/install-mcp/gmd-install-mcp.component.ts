/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';

import { GMD_MCP_INSTALLERS } from './gmd-install-mcp.tokens';
import { McpServerSpec } from '../../models/mcpServerSpec';
import { GmdButtonComponent } from '../button/gmd-button.component';

type StringMap = Record<string, string>;
type StringInput<T> = T | string | undefined;

@Component({
  selector: 'gmd-install-mcp',
  standalone: true,
  imports: [CommonModule, CdkCopyToClipboard, GmdButtonComponent],
  templateUrl: './gmd-install-mcp.component.html',
  styleUrl: './gmd-install-mcp.component.scss',
})
export class GmdInstallMcpComponent {
  private readonly installers = inject(GMD_MCP_INSTALLERS);

  name = input<string | undefined>();
  transport = input<'http' | 'sse' | 'stdio'>('http');
  url = input<string | undefined>();
  headers = input<StringInput<StringMap>>();
  command = input<string | undefined>();
  args = input<StringInput<string[]>>();
  env = input<StringInput<StringMap>>();
  clients = input<string | undefined>();

  protected readonly activeClientId = signal<string | null>(null);
  protected readonly snippetCopied = signal(false);

  protected readonly requestedClientIds = computed(() =>
    (this.clients() ?? '')
      .split(',')
      .map(clientId => clientId.trim())
      .filter(Boolean),
  );
  protected readonly resolvedSpec = computed(() => this.buildExplicitSpec());
  protected readonly availableInstallers = computed(() => {
    const resolvedSpec = this.resolvedSpec();
    if (!resolvedSpec) {
      return [];
    }

    const requestedClientIds = this.requestedClientIds();
    return this.installers.filter(installer => {
      if (requestedClientIds.length > 0 && !requestedClientIds.includes(installer.id)) {
        return false;
      }

      return installer.supports(resolvedSpec);
    });
  });
  protected readonly selectedInstaller = computed(() => {
    const availableInstallers = this.availableInstallers();
    const activeClientId = this.activeClientId();

    if (!availableInstallers.length) {
      return null;
    }

    return availableInstallers.find(installer => installer.id === activeClientId) ?? availableInstallers[0];
  });
  protected readonly deepLink = computed(() => {
    const selectedInstaller = this.selectedInstaller();
    const resolvedSpec = this.resolvedSpec();

    return selectedInstaller?.buildDeepLink && resolvedSpec ? selectedInstaller.buildDeepLink(resolvedSpec) : null;
  });
  protected readonly snippet = computed(() => {
    const selectedInstaller = this.selectedInstaller();
    const resolvedSpec = this.resolvedSpec();

    return selectedInstaller && resolvedSpec ? selectedInstaller.buildSnippet(resolvedSpec) : '';
  });
  protected readonly snippetFileName = computed(() => this.selectedInstaller()?.snippetFileName ?? 'mcp.json');
  protected readonly placeholderMessage = computed(() => {
    if (!this.resolvedSpec()) {
      return 'Provide a server name and URL, or use stdio inputs for a local MCP server.';
    }

    return 'No supported installers are available for the selected clients.';
  });

  private readonly syncSelectedInstaller = effect(() => {
    const availableInstallers = this.availableInstallers();
    const activeClientId = this.activeClientId();

    if (!availableInstallers.length) {
      this.activeClientId.set(null);
      return;
    }

    if (!activeClientId || !availableInstallers.some(installer => installer.id === activeClientId)) {
      this.activeClientId.set(availableInstallers[0].id);
    }
  });

  selectClient(clientId: string): void {
    this.activeClientId.set(clientId);
  }

  onSnippetCopied(): void {
    this.snippetCopied.set(true);
    setTimeout(() => this.snippetCopied.set(false), 2000);
  }

  private buildExplicitSpec(): McpServerSpec | null {
    const name = this.name()?.trim();
    if (!name) {
      return null;
    }

    const transport = this.transport();
    if (transport === 'stdio') {
      const command = this.command()?.trim();
      if (!command) {
        return null;
      }

      return {
        name,
        transport,
        command,
        args: this.parseStringArrayInput(this.args()),
        env: this.parseStringMapInput(this.env()),
      };
    }

    const url = this.url()?.trim();
    if (!url) {
      return null;
    }

    return {
      name,
      transport,
      url,
      headers: this.parseStringMapInput(this.headers()),
    };
  }

  private parseStringArrayInput(value: StringInput<string[]>): string[] | undefined {
    if (Array.isArray(value)) {
      return value;
    }

    if (!value) {
      return undefined;
    }

    try {
      const parsedValue = JSON.parse(value);
      if (Array.isArray(parsedValue) && parsedValue.every(item => typeof item === 'string')) {
        return parsedValue;
      }
    } catch {
      // Fall back to comma-separated strings for simple GMD authoring.
    }

    return value
      .split(',')
      .map(item => item.trim())
      .filter(Boolean);
  }

  private parseStringMapInput(value: StringInput<StringMap>): StringMap | undefined {
    if (!value) {
      return undefined;
    }

    if (typeof value !== 'string') {
      return value;
    }

    try {
      const parsedValue = JSON.parse(value);
      if (parsedValue && typeof parsedValue === 'object' && !Array.isArray(parsedValue)) {
        return Object.entries(parsedValue).reduce<StringMap>((acc, [key, entryValue]) => {
          if (typeof entryValue === 'string') {
            acc[key] = entryValue;
          }
          return acc;
        }, {});
      }
    } catch {
      return undefined;
    }

    return undefined;
  }
}
