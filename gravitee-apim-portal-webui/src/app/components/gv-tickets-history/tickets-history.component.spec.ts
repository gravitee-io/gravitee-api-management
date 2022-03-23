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
import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';

import { TicketsHistoryComponent } from './tickets-history.component';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateTestingModule } from '../../test/translate-testing-module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateService } from '@ngx-translate/core';
import { ConfigurationService } from '../../services/configuration.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { ScrollService } from '../../services/scroll.service';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { PortalService, Ticket, TicketsResponse } from '../../../../projects/portal-webclient-sdk/src/lib';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('GvTicketsHistoryComponent', () => {
  let route;
  let portalService;
  let routerSpy;

  const PAGE_SIZES = [5, 10, 15];
  const PAGE_SIZE_DEFAULT = 10;

  const tickets: Ticket[] = [
    { id: 'ticket1', subject: 'subject1', content: 'content1', api: 'api1', application: 'app1', created_at: new Date() },
    { id: 'ticket2', subject: 'subject2', content: 'content2', api: 'api1', application: 'app1', created_at: new Date() },
  ];

  const response: TicketsResponse = {
    data: tickets,
    metadata: {
      data: { total: { total: tickets.length } },
    },
  };

  const createComponent = createComponentFactory({
    component: TicketsHistoryComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule, TranslateTestingModule, HttpClientTestingModule],
    providers: [mockProvider(TranslateService)],
  });

  let spectator: Spectator<TicketsHistoryComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  beforeEach(waitForAsync(() => {
    route = spectator.inject(ActivatedRoute);
    portalService = spectator.inject(PortalService);
    routerSpy = spectator.inject(Router);

    jest.spyOn(ConfigurationService.prototype, 'get').mockImplementation(arg => {
      if (arg === 'pagination.size.values') {
        return PAGE_SIZES;
      }
    });

    route.queryParams = of({
      skipRefresh: false,
      size: 10,
      page: 1,
      field: 'subject',
      order: 'ASC',
    });

    route.snapshot = {
      queryParams: {
        size: 10,
      },
      data: {
        api: { id: 'apiId' },
      },
    };
  }));

  afterEach(() => {
    if (component.queryParamSubscription) {
      component.ngOnDestroy();
    }
  });

  it('should init the component', () => {
    jest.spyOn(component, 'refresh');

    component.ngOnInit();

    expect(component).toBeTruthy();
    expect(component.compareFn).toBeDefined();
    expect(component.pageSizes).toEqual(PAGE_SIZES);
    expect(component.size).toEqual(PAGE_SIZE_DEFAULT);

    expect(component.refresh).toHaveBeenCalled();
  });

  it('should get ticket data without api path param and without selected ticket', async () => {
    jest.spyOn(portalService, 'getTickets').mockReturnValue(of(response));
    route.snapshot.data.api = null;
    route.queryParams.ticket = null;

    await component.refresh(route.queryParams);

    expect(component.tickets.length).toEqual(tickets.length);
    expect(component.selectedTicket).toBeNull();
    expect(component.selectedTicketIds.length).toEqual(0);
    expect(component.options.data.length).toEqual(4);
  });

  it('should get ticket data with api path param and with selected ticket', async () => {
    jest.spyOn(portalService, 'getTickets').mockReturnValue(of(response));
    const scrollServiceSpy = jest.spyOn(ScrollService.prototype, 'scrollToAnchor').mockResolvedValue();
    route.queryParams.ticket = 'ticket1';

    await component.refresh(route.queryParams);

    expect(component.tickets.length).toEqual(tickets.length);
    expect(component.selectedTicket.id).toEqual('ticket1');
    expect(component.selectedTicketIds.length).toEqual(1);
    expect(component.options.data.length).toEqual(3);
    expect(scrollServiceSpy).toHaveBeenCalledWith('ticket');
  });

  it('should get field order for desc order query', () => {
    route.snapshot.queryParams.order = '-subject';

    expect(component.getOrder()).toEqual('-subject');
  });

  it('should return default field order when no queried', () => {
    expect(component.getOrder()).toEqual('-created_at');
  });

  it('should navigate with new query params when selecting page size', fakeAsync(() => {
    const spy = jest.spyOn(routerSpy, 'navigate').mockReturnValue(Promise.resolve(true));

    component.onSelectSize(5);

    tick();

    expect(spy).toHaveBeenCalledWith([], {
      queryParams: { size: 5, page: null, ticket: null },
      queryParamsHandling: 'merge',
    });
    expect(component.size).toEqual(5);
  }));

  it('should navigate with new query params when paginating', fakeAsync(() => {
    component.paginationData = {
      current_page: 1,
    };
    component.size = 5;
    const spy = jest.spyOn(routerSpy, 'navigate').mockReturnValue(Promise.resolve(true));

    component._onPaginate({ page: 2 });

    expect(spy).toHaveBeenCalledWith([], {
      queryParams: {
        page: 2,
        size: 5,
        ticket: null,
      },
      queryParamsHandling: 'merge',
    });
  }));

  it('should navigate with new query params when sorting', fakeAsync(() => {
    const spy = jest.spyOn(routerSpy, 'navigate').mockReturnValue(Promise.resolve(true));

    component._onSort({ order: '-subject' });

    expect(spy).toHaveBeenCalledWith([], {
      queryParams: {
        order: '-subject',
        ticket: null,
      },
      queryParamsHandling: 'merge',
    });
  }));

  it('should navigate with new query params when selecting a ticket', fakeAsync(() => {
    const spy = jest.spyOn(routerSpy, 'navigate').mockReturnValue(Promise.resolve(true));

    component.onSelectTicket(tickets[0]);

    expect(spy).toHaveBeenCalledWith([], {
      queryParams: {
        ticket: tickets[0].id,
      },
      queryParamsHandling: 'merge',
    });
  }));

  it('should not navigate with new query params when deselecting a ticket', fakeAsync(() => {
    const spy = jest.spyOn(routerSpy, 'navigate').mockReturnValue(Promise.resolve(true));

    component.onSelectTicket(undefined);

    expect(spy).not.toHaveBeenCalled();
    expect(component.selectedTicket).toBeNull();
    expect(component.selectedTicketIds.length).toEqual(0);
  }));
});
