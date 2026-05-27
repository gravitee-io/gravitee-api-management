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
import { ComponentHarness } from '@angular/cdk/testing';

export class GmdInstallMcpComponentHarness extends ComponentHarness {
  static hostSelector = 'gmd-install-mcp';

  async getSnippetText(): Promise<string> {
    const snippet = await this.locatorFor('[data-testid="gmd-install-mcp-snippet"]')();
    return snippet.text();
  }

  async getPlaceholderText(): Promise<string | null> {
    const placeholder = await this.locatorForOptional('[data-testid="gmd-install-mcp-placeholder"]')();
    return placeholder ? placeholder.text() : null;
  }

  async clickClient(clientId: string): Promise<void> {
    const clientButton = await this.locatorFor(`[data-testid="gmd-install-mcp-client-${clientId}"]`)();
    return clientButton.click();
  }

  async hasClient(clientId: string): Promise<boolean> {
    const clientButton = await this.locatorForOptional(`[data-testid="gmd-install-mcp-client-${clientId}"]`)();
    return clientButton !== null;
  }

  async getInstallHref(): Promise<string | null> {
    const installButton = await this.locatorForOptional('[data-testid="gmd-install-mcp-install-link"] a')();
    return installButton?.getAttribute('href') ?? null;
  }

  async getFallbackHref(): Promise<string | null> {
    const fallbackButton = await this.locatorForOptional('[data-testid="gmd-install-mcp-fallback-link"] a')();
    return fallbackButton?.getAttribute('href') ?? null;
  }

  async copySnippet(): Promise<void> {
    const copyButton = await this.locatorFor('[data-testid="gmd-install-mcp-copy"]')();
    return copyButton.click();
  }

  async getCopyButtonText(): Promise<string | null> {
    const copyButton = await this.locatorForOptional('[data-testid="gmd-install-mcp-copy"]')();
    return copyButton ? copyButton.text() : null;
  }
}
