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

const SearchAndSelectComponent: ng.IComponentOptions = {
  template: require('./search-and-select.html'),
  controller: 'SearchAndSelectController',
  bindings: {
    selectModel: '=', // md-select model (a list of id strings)
    init: '&', // this function can be used to initialize the selection (from e.g. URL query parameters)
    search: '&', // this function is called on input in the search field to update the list of options
    onSelect: '&', // this function can be used by the parent component to react on selection changes
    context: '<', // used to initialize labels and placeholders depending on the  kind of entity (e.g. 'API')
  },
};

export default SearchAndSelectComponent;
