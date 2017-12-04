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
import * as _ from "lodash";

class ViewService {
  private viewsURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.viewsURL = `${Constants.baseURL}configuration/views/`;
  }

  list(all?: boolean) {
    const url = all ? this.viewsURL + "?all=true" : this.viewsURL;
    return this.$http.get(url);
  }

  create(views) {
    if (views && views.length) {
      return this.$http.post(this.viewsURL, views);
    }
  }

  update(views) {
    if (views && views.length) {
      return this.$http.put(this.viewsURL, views);
    }
  }

  getDefault() {
    return this.$http.get(this.viewsURL + "/default");
  }

  getDefaultOrFirstOne() {
    return this.getDefault().then(response => {
      if (response.data.totalApis > 0) {
        return response.data;
      } else {
        return this.list().then(response => {
          let viewsWithApis = _.filter(response.data, (view: any) => { return view.totalApis > 0 });
          if (viewsWithApis && viewsWithApis.length > 0) {
            return viewsWithApis[0];
          } else {
            return {};
          }
        });
      }
    });
  }

  delete(view) {
    if (view) {
      return this.$http.delete(this.viewsURL + view.id);
    }
  }
}

export default ViewService;
