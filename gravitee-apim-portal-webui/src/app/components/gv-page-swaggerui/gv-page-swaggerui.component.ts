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
import { Component, OnInit } from '@angular/core';
import { PlatformLocation } from '@angular/common';
import SwaggerUI, { SwaggerUIOptions, SwaggerUIPlugin } from 'swagger-ui';
import * as React from 'react';

import { Page, User } from '../../../../projects/portal-webclient-sdk/src/lib';
import { CurrentUserService } from '../../services/current-user.service';
import { PageService } from '../../services/page.service';
import { readYaml } from '../../utils/yaml-parser';

type docExpansion = 'list' | 'full' | 'none';

@Component({
  selector: 'app-gv-page-swaggerui',
  templateUrl: './gv-page-swaggerui.component.html',
  styleUrls: ['./gv-page-swaggerui.component.css'],
})
export class GvPageSwaggerUIComponent implements OnInit {
  currentUser: User;

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

  private refresh(page: Page) {
    if (page) {
      const ui = SwaggerUI({
        dom_id: '#swagger',
        ...this.buildConfig(page),
      });
      ui.initOAuth({ usePkceWithAuthorizationCodeGrant: page.configuration?.use_pkce });
    }
  }

  private buildConfig(page: Page): SwaggerUIOptions {
    const spec = this.readSpec(page);
    const plugins = this.buildPlugins(page);

    const config: SwaggerUIOptions = {
      dom_id: '#swagger',
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
      return JSON.parse(page.content);
    } catch (_) {
      return readYaml(page.content);
    }
  }

  buildPlugins(page: Page): SwaggerUIPlugin[] {
    const plugins: SwaggerUIPlugin[] = [];
    if (!this.isTryItEnabled(page)) {
      plugins.push(this.disabledTryItOutPlugin);
      plugins.push(this.disabledAuthorizationPlugin);
    } else {
      plugins.push(this.disableHighlightPlugin());
    }
    return plugins;
  }

  disableHighlightPlugin(): SwaggerUIPlugin {
    interface Props {
      children?: React.ReactNode;
    }

    class EmptyHighlightCode extends React.Component<Props> {
      declare props: Props;

      render(): React.ReactNode {
        return React.createElement(
          'pre',
          {
            style: {
              backgroundColor: '#2d2d2d',
              color: '#eee',
              maxHeight: '400px',
              overflow: 'auto',
              whiteSpace: 'pre',
            },
          },
          this.props?.children || '',
        );
      }
    }

    return {
      components: {
        HighlightCode: EmptyHighlightCode,
      },
    } as any;
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
