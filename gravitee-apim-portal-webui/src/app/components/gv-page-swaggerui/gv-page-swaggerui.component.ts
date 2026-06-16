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
import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PlatformLocation } from '@angular/common';
import SwaggerUI, { SwaggerUIOptions, SwaggerUIPlugin } from 'swagger-ui';

import { Page, User } from '../../../../projects/portal-webclient-sdk/src/lib';
import { CurrentUserService } from '../../services/current-user.service';
import { PageService } from '../../services/page.service';
import { readYaml } from '../../utils/yaml-parser';

const OAS_SCHEMA_TYPES = new Set(['null', 'boolean', 'object', 'array', 'number', 'string', 'integer']);
// Priority order: most permissive first, so we pick the least-restrictive type from the array
const OAS_TYPE_PRIORITY = ['string', 'number', 'integer', 'boolean', 'array', 'object'];

type docExpansion = 'list' | 'full' | 'none';

@Component({
  selector: 'app-gv-page-swaggerui',
  templateUrl: './gv-page-swaggerui.component.html',
  styleUrls: ['./gv-page-swaggerui.component.css'],
  standalone: false,
})
export class GvPageSwaggerUIComponent implements OnInit, OnDestroy {
  currentUser: User;
  @ViewChild('swaggerContainer', { static: true }) private swaggerContainer: ElementRef<HTMLDivElement>;

  constructor(
    private currentUserService: CurrentUserService,
    private pageService: PageService,
    private platformLocation: PlatformLocation,
  ) {}

  ngOnInit() {
    this.currentUserService.get().subscribe(newCurrentUser => {
      this.currentUser = newCurrentUser;
    });
    this.refresh(this.pageService.getCurrentPage());
  }

  ngOnDestroy() {
    if (this.swaggerContainer?.nativeElement) {
      this.swaggerContainer.nativeElement.innerHTML = '';
    }

    // Clean up global side effects leakage from SwaggerUI / React
    document.getElementById('preact-border-shadow-host')?.remove();

    // Restore body overflow - Swagger UI sets overflow:hidden when opening schema modals,
    // which can persist and break the layout when navigating back to homepage
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  }

  private refresh(page: Page) {
    if (page && this.swaggerContainer?.nativeElement) {
      const ui = SwaggerUI({
        domNode: this.swaggerContainer.nativeElement,
        ...this.buildConfig(page),
      });
      ui.initOAuth({ usePkceWithAuthorizationCodeGrant: page.configuration?.use_pkce });
    }
  }

  private buildConfig(page: Page): SwaggerUIOptions {
    const spec = this.readSpec(page);
    const plugins = this.buildPlugins(page);

    const config: SwaggerUIOptions = {
      defaultModelsExpandDepth: 0,
      layout: 'BaseLayout',
      plugins,
      requestInterceptor: req => {
        if (req.loadSpec) {
          req.credentials = 'include';
        }
        return req;
      },
      spec,
      oauth2RedirectUrl: window.location.origin + this.platformLocation.getBaseHrefFromDOM() + 'oauth2-redirect.html',
    };

    if (page.configuration) {
      if (page.configuration.show_url) {
        config.url = page._links.content;
        config.spec = undefined;
        plugins.push(this.normalizeSpecPlugin());
      }
      if (page.configuration?.disable_syntax_highlight) {
        config.syntaxHighlight = false;
      }
      config.docExpansion = this.getDocExpansion(page);
      config.displayOperationId = page.configuration.display_operation_id || false;
      config.filter = page.configuration.enable_filtering || false;
      config.showExtensions = page.configuration.show_extensions || false;
      config.showCommonExtensions = page.configuration.show_common_extensions || false;
      config.maxDisplayedTags = page.configuration.max_displayed_tags < 0 ? undefined : page.configuration.max_displayed_tags;
    }

    return config;
  }

  private readSpec(page: Page): { [propName: string]: any } | undefined {
    try {
      return this.normalizeTypeArrays(JSON.parse(page.content));
    } catch (_) {
      return this.normalizeTypeArrays(readYaml(page.content));
    }
  }

  /**
   * Recursively normalizes OAS 3.1 type arrays (e.g. `"type": ["integer", "string"]`) to a single
   * string type that Swagger UI can validate correctly. Without this, Swagger UI's form validator
   * fails to match entered values against an array type and incorrectly reports required fields as
   * missing. See APIM-14231.
   */
  private normalizeTypeArrays(obj: any): any {
    if (obj === null || typeof obj !== 'object') {
      return obj;
    }
    if (Array.isArray(obj)) {
      return obj.map((item: any) => this.normalizeTypeArrays(item));
    }
    const result: any = {};
    for (const key of Object.keys(obj)) {
      if (key === 'type' && Array.isArray(obj[key]) && (obj[key] as string[]).every((t: string) => OAS_SCHEMA_TYPES.has(t))) {
        const nonNullTypes = (obj[key] as string[]).filter((t: string) => t !== 'null');
        result[key] = nonNullTypes.length === 1 ? nonNullTypes[0] : (OAS_TYPE_PRIORITY.find(t => nonNullTypes.includes(t)) ?? 'string');
        if ((obj[key] as string[]).includes('null')) {
          result['nullable'] = true;
        }
      } else {
        result[key] = this.normalizeTypeArrays(obj[key]);
      }
    }
    return result;
  }

  buildPlugins(page: Page): SwaggerUIPlugin[] {
    const plugins: SwaggerUIPlugin[] = [];
    if (!this.isTryItEnabled(page)) {
      plugins.push(this.disabledTryItOutPlugin);
      plugins.push(this.disabledAuthorizationPlugin);
    }
    return plugins;
  }

  disabledTryItOutPlugin() {
    return {
      statePlugins: {
        spec: {
          wrapSelectors: {
            allowTryItOutFor: () => () => false,
          },
        },
      },
    };
  }

  disabledAuthorizationPlugin() {
    return {
      wrapComponents: {
        authorizeBtn: () => () => null,
      },
    };
  }

  private normalizeSpecPlugin(): SwaggerUIPlugin {
    const normalize = (obj: any) => this.normalizeTypeArrays(obj);
    const origin = window.location.origin;
    return () => ({
      statePlugins: {
        spec: {
          wrapActions: {
            updateJsonSpec: (oriAction: (spec: Record<string, any>) => void) => (spec: Record<string, any>) => {
              const normalized = normalize(spec);
              this.resolveRelativeServerUrls(normalized, origin);
              return oriAction(normalized);
            },
          },
        },
      },
    });
  }

  /**
   * When the spec is loaded via URL (show_url), Swagger UI resolves relative server URLs against
   * the management API origin instead of the portal origin. This causes "Try it out" requests to
   * hit the wrong host. Fix by resolving relative URLs against the portal origin, matching the
   * behavior when the spec is loaded inline (show_url disabled). See APIM-14431.
   */
  private resolveRelativeServerUrls(spec: Record<string, any>, origin: string): void {
    if (Array.isArray(spec['servers'])) {
      spec['servers'] = (spec['servers'] as Record<string, any>[]).map(server => {
        const url = (server['url'] as string) ?? '/';
        if (url.startsWith('/')) {
          return { ...server, url: origin + url };
        }
        return server;
      });
    }
  }

  private isTryItEnabled(page: Page) {
    return page.configuration?.try_it && this.isTryItGranted(page);
  }

  private isTryItGranted(page: Page) {
    return !!this.currentUser || page.configuration?.try_it_anonymous;
  }

  private getDocExpansion(page: Page): docExpansion {
    if (page.configuration?.doc_expansion) {
      return page.configuration.doc_expansion.toLocaleLowerCase() as docExpansion;
    }
    return 'none';
  }
}
