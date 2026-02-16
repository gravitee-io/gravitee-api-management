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
import { forEach } from 'lodash';

export class TicketsQuery {
  page: number;
  size: number;
  order: string;
}

class TicketService {
  constructor(
    private $http,
    private Constants,
  ) {}

  create(ticket) {
    if (ticket) {
      return this.$http.post(`${this.Constants.env.baseURL}/platform/tickets`, ticket);
    }
  }

  search(query: TicketsQuery) {
    return this.$http.get(this.buildURLWithQuery(query, `${this.Constants.env.baseURL}/platform/tickets` + '?'));
  }

  getTicket(ticketId: string) {
    return this.$http.get(`${this.Constants.env.baseURL}/platform/tickets/${ticketId}`);
  }

  private buildURLWithQuery(query: TicketsQuery, url) {
    const keys = Object.keys(query);
    forEach(keys, key => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }
}
TicketService.$inject = ['$http', 'Constants'];

export default TicketService;
