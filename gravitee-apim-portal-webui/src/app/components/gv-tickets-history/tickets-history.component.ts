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
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import '@gravitee/ui-components/wc/gv-select';
import '@gravitee/ui-components/wc/gv-table';
import { SearchQueryParam } from '../../utils/search-query-param.enum';
import { ConfigurationService } from '../../services/configuration.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { ScrollService } from '../../services/scroll.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { GetTicketsRequestParams, PortalService, Ticket } from '../../../../projects/portal-webclient-sdk/src/lib';

@Component({
  selector: 'app-gv-tickets-history',
  templateUrl: './tickets-history.component.html',
  styleUrls: ['./tickets-history.component.css'],
})
export class TicketsHistoryComponent implements OnInit, OnDestroy {
  queryParamSubscription: Subscription;
  tickets: Ticket[] = [];
  selectedTicket: Ticket;
  options: any;
  format: any;
  paginationData: any = {};
  pageSizes: Array<any>;
  size: number;
  api: string;
  selectedTicketIds: string[];
  compareFn: any;

  constructor(
    private config: ConfigurationService,
    private portalService: PortalService,
    private translateService: TranslateService,
    private scrollService: ScrollService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.pageSizes = this.config.get('pagination.size.values');
    this.size = this.route.snapshot.queryParams[SearchQueryParam.SIZE]
      ? parseInt(this.route.snapshot.queryParams[SearchQueryParam.SIZE], 10)
      : this.config.get('pagination.size.default');

    // since the content of the table is already sorted by the backend, we don't need to sort the table again
    this.compareFn = () => {};

    this.queryParamSubscription = this.route.queryParams.subscribe((queryParams) => {
      if (queryParams && !queryParams.skipRefresh) {
        this.refresh(queryParams);
      }
    });
  }

  ngOnDestroy() {
    this.queryParamSubscription.unsubscribe();
  }

  async refresh(queryParams) {
    this.api = this.route.snapshot.data.api;
    const response = await this.portalService.getTickets(this.getRequestParameters(queryParams, this.api)).toPromise();
    this.tickets = response.data;

    this.buildPaginationData(response.metadata.data.total);
    this.format = (key) => this.translateService.get(key).toPromise();

    this.initTableOptions();

    if (queryParams.ticket) {
      this.selectedTicketIds = this.tickets.filter((t) => t.id === queryParams.ticket).map((ticket) => ticket.id);
      this.loadTicket(queryParams.ticket);
    } else {
      this.selectedTicket = null;
      this.selectedTicketIds = [];
    }
  }

  getOrder() {
    return this.route.snapshot.queryParams[SearchQueryParam.ORDER] || '-created_at';
  }

  onSelectSize(size) {
    const queryParams: any = {};
    queryParams[SearchQueryParam.SIZE] = size;
    queryParams.ticket = null;
    this.router
      .navigate([], {
        queryParams: { size, page: null, ticket: null },
        queryParamsHandling: 'merge',
      })
      .then(() => {
        this.size = size;
      });
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    const queryParams: any = {};
    queryParams[SearchQueryParam.PAGE] = page;
    queryParams[SearchQueryParam.SIZE] = this.size;
    queryParams.ticket = null;
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  @HostListener(':gv-table:sort', ['$event.detail'])
  _onSort({ order }) {
    const queryParams: any = {};
    queryParams[SearchQueryParam.ORDER] = order;
    queryParams.ticket = null;
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  @HostListener(':gv-table:select', ['$event.detail.items[0]'])
  onSelectTicket(ticket: Ticket) {
    if (ticket) {
      this.router.navigate([], {
        queryParams: { ticket: ticket ? ticket.id : null },
        queryParamsHandling: 'merge',
      });
    } else {
      this.selectedTicket = null;
      this.selectedTicketIds = [];
    }
  }

  private loadTicket(ticketId) {
    this.selectedTicket = this.tickets.find((t) => t.id === ticketId);
    this.scrollService.scrollToAnchor('ticket');
  }

  private getRequestParameters(queryParams, api): GetTicketsRequestParams {
    return {
      apiId: api ? api.id : null,
      size: queryParams[SearchQueryParam.SIZE] || this.size,
      page: queryParams[SearchQueryParam.PAGE] || 1,
      order: queryParams[SearchQueryParam.ORDER] || '-created_at',
    };
  }

  private buildPaginationData(total) {
    const totalPages = total / this.size;
    this.paginationData = {
      first: 1,
      last: totalPages,
      current_page: this.route.snapshot.queryParams[SearchQueryParam.PAGE] || 1,
      total_pages: totalPages,
      total,
    };
  }

  private initTableOptions() {
    const data: any[] = [
      { field: 'created_at', type: 'datetime', label: i18n('tickets.date'), width: '200px' },
      { field: 'application', label: i18n('tickets.application') },
      { field: 'subject', label: i18n('tickets.subject') },
    ];

    if (!this.api) {
      data.splice(1, 0, { field: 'api', label: i18n('tickets.api') });
    }

    this.options = {
      selectable: true,
      data,
    };
  }
}
