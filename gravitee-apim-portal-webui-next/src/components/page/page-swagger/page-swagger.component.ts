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
