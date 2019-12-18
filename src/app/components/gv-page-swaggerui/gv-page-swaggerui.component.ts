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
import * as jsyaml from 'js-yaml';

import { Component, Input, OnInit } from '@angular/core';
import { ApiService, Page, PortalService, User } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute } from '@angular/router';

import { SwaggerUIBundle } from 'swagger-ui-dist';
import { CurrentUserService } from 'src/app/services/current-user.service';

@Component({
  selector: 'app-gv-page-swaggerui',
  templateUrl: './gv-page-swaggerui.component.html',
  styleUrls: ['./gv-page-swaggerui.component.css']
})
export class GvPageSwaggerUIComponent implements OnInit {

  @Input() set page(page: Page) {
    if (page) {
      const apiId = this.route.snapshot.params.apiId;
      if (apiId) {
        this.apiService.getPageByApiIdAndPageId({ apiId, pageId: page.id, include: ['content'] }).subscribe(response => {
          this.refresh(response);
        });
      } else {
        this.portalService.getPageByPageId({ pageId: page.id, include: ['content'] }).subscribe(response => {
          this.refresh(response);
        });
      }
    }
  }

  constructor(
    private portalService: PortalService,
    private apiService: ApiService,
    private route: ActivatedRoute,
    private currentUserService: CurrentUserService
    ) { }

  currentPage: Page;
  currentUser: User;

  ngOnInit() {
    this.currentUserService.get().subscribe(newCurrentUser => {
      this.currentUser = newCurrentUser;
    });
  }

  DisableTryItOutPlugin() {
    return {
      statePlugins: {
        spec: {
          wrapSelectors: {
            allowTryItOutFor: () => () => false
          }
        }
      }
    };
  }

  refresh(page: Page) {
    const cfg: any = this._prepareConfig(page);
    SwaggerUIBundle({ ...cfg, ...{ onComplete: () => {
        this.currentPage = page;
      }
    }});
  }

  _prepareConfig(page: Page) {
    const customPlugins = [];
    if (page.configuration
        && !page.configuration.try_it
        && (this.currentUser || !page.configuration.try_it_anonymous)) {
          customPlugins.push(this.DisableTryItOutPlugin);
    }

    let contentAsJson = {};
    try {
      contentAsJson = JSON.parse(page.content);
    } catch (e) {
      contentAsJson = jsyaml.safeLoad(page.content);
    }

    const cfg: any = {
      dom_id: '#swagger',
      defaultModelsExpandDepth: 0,
      presets: [
        SwaggerUIBundle.presets.apis,
      ],
      layout: 'BaseLayout',
      plugins: customPlugins,
      requestInterceptor: (req) => {
        if (req.loadSpec) {
          req.credentials = 'include';
        }
        return req;
      },
      spec: contentAsJson,
      oauth2RedirectUrl: window.location.origin + window.location.pathname + (window.location.pathname.substr(-1) !== '/' ? '/' : '') + 'swagger-oauth2-redirect.html',
    };

    if (page.configuration) {
      if (page.configuration.show_url) {
        cfg.url = page._links.content;
        cfg.spec = undefined;
      }
      cfg.docExpansion = page.configuration.doc_expansion ? page.configuration.doc_expansion.toLocaleLowerCase() : 'none';
      cfg.displayOperationId = page.configuration.display_operation_id || false;
      cfg.filter = page.configuration.enable_filtering || false;
      cfg.showExtensions = page.configuration.show_extensions || false;
      cfg.showCommonExtensions = page.configuration.show_common_extensions || false;
      cfg.maxDisplayedTags = page.configuration.max_displayed_tags < 0
          ? undefined : page.configuration.max_displayed_tags;
    }

    return cfg;
  }
}
