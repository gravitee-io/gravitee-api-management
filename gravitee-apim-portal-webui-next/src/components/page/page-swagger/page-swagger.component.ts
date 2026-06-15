/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { PlatformLocation } from '@angular/common';
import { Component, inject, Input, OnChanges } from '@angular/core';
import SwaggerUI, { SwaggerUIOptions, SwaggerUIPlugin } from 'swagger-ui';

import { readYaml } from '../../../app/helpers/yaml-parser';
import { Page } from '../../../entities/page/page';
import { CurrentUserService } from '../../../services/current-user.service';

@Component({
  selector: 'app-page-swagger',
  standalone: true,
  template: `<div id="swagger"></div>`,
})
export class PageSwaggerComponent implements OnChanges {
  @Input() page!: Page;
  private platformLocation: PlatformLocation = inject(PlatformLocation);

  constructor(private currentUser: CurrentUserService) {}

  ngOnChanges() {
    SwaggerUI({
      domNode: document.getElementById('swagger'),
      spec: this.readSpec() ?? '',
      ...this.buildConfig(),
    }).initOAuth({ usePkceWithAuthorizationCodeGrant: this.page.configuration?.use_pkce });
  }

  private buildConfig(): SwaggerUIOptions {
    const plugins = this.buildPlugins();

    const config: SwaggerUIOptions = {
      defaultModelsExpandDepth: 0,
      layout: 'BaseLayout',
      plugins,
      requestInterceptor: req => {
        if (req['loadSpec']) {
          req['credentials'] = 'include';
        }
        return req;
      },
      oauth2RedirectUrl: `${window.location.origin}${this.platformLocation.getBaseHrefFromDOM()}oauth2-redirect.html`,
    };

    if (this.page.configuration) {
      const pageConfiguration = this.page.configuration;
      if (pageConfiguration.show_url) {
        config.url = this.page._links?.content;
        config.spec = undefined;
        plugins.push(this.normalizeSpecPlugin());
      }
      if (this.page.configuration?.disable_syntax_highlight) {
        config.syntaxHighlight = false;
      }
      config.docExpansion = this.page.configuration?.doc_expansion ?? 'none';
      config.displayOperationId = pageConfiguration.display_operation_id || false;
      config.filter = pageConfiguration.enable_filtering || false;
      config.showExtensions = pageConfiguration.show_extensions || false;
      config.showCommonExtensions = pageConfiguration.show_common_extensions || false;
      config.maxDisplayedTags =
        pageConfiguration.max_displayed_tags && pageConfiguration.max_displayed_tags >= 0
          ? pageConfiguration.max_displayed_tags
          : undefined;
    }

    return config;
  }

  private buildPlugins(): SwaggerUIPlugin[] {
    const plugins: SwaggerUIPlugin[] = [];
    if (!this.isTryItEnabled()) {
      plugins.push(this.disabledTryItOutPlugin);
      plugins.push(this.disabledAuthorizationPlugin);
    }
    return plugins;
  }

  private readSpec(): { [propName: string]: unknown } | unknown {
    const content = this.page.content;
    if (!content) {
      return undefined;
    }
    try {
      return JSON.parse(content);
    } catch (_) {
      return readYaml(content);
    }
  }

<<<<<<< HEAD
=======
  /**
   * Recursively normalizes OAS 3.1 type arrays (e.g. `"type": ["integer", "string"]`) to a single
   * string type that Swagger UI can validate correctly. Without this, Swagger UI's form validator
   * fails to match entered values against an array type and incorrectly reports required fields as
   * missing. See APIM-14231.
   */
  private normalizeTypeArrays(obj: unknown): unknown {
    if (obj === null || typeof obj !== 'object') {
      return obj;
    }
    if (Array.isArray(obj)) {
      return obj.map(item => this.normalizeTypeArrays(item));
    }
    const record = obj as Record<string, unknown>;
    const result: Record<string, unknown> = {};
    for (const key of Object.keys(record)) {
      if (key === 'type' && Array.isArray(record[key]) && (record[key] as string[]).every(t => OAS_SCHEMA_TYPES.has(t))) {
        const typeArray = record[key] as string[];
        const nonNullTypes = typeArray.filter(t => t !== 'null');
        result[key] = nonNullTypes.length === 1 ? nonNullTypes[0] : (OAS_TYPE_PRIORITY.find(t => nonNullTypes.includes(t)) ?? 'string');
        if (typeArray.includes('null')) {
          result['nullable'] = true;
        }
      } else {
        result[key] = this.normalizeTypeArrays(record[key]);
      }
    }
    return result;
  }

  private normalizeSpecPlugin(): SwaggerUIPlugin {
    const normalize = (obj: unknown) => this.normalizeTypeArrays(obj);
    return () => ({
      statePlugins: {
        spec: {
          wrapActions: {
            updateJsonSpec: (oriAction: (spec: Record<string, unknown>) => void) => (spec: Record<string, unknown>) => {
              return oriAction(normalize(spec) as Record<string, unknown>);
            },
          },
        },
      },
    });
  }

>>>>>>> dfdd2b3875 (fix(portal): normalize OAS 3.1 type arrays when show_url is enabled)
  private disabledTryItOutPlugin() {
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

  private disabledAuthorizationPlugin() {
    return {
      wrapComponents: {
        authorizeBtn: () => () => null,
      },
    };
  }

  private isTryItEnabled() {
    return this.page.configuration?.try_it && this.isTryItAllowed();
  }

  private isTryItAllowed() {
    return this.currentUser.isAuthenticated() || this.page.configuration?.try_it_anonymous;
  }
}
