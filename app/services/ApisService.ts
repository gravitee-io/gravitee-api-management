///
/// Copyright (C) 2015 The Gravitee team (http://gravitee.io)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {Api} from '../components/apis/api';

declare var fetch;

export class ApisService {

    constructor() {
    }

    list() {
        return fetch('http://localhost:8059/rest/apis').then(r => r.json());
    }

    start(name: string) {
        return fetch('http://localhost:8059/rest/apis/start/' + name, {mode: 'no-cors', method:'POST'});
    }

    stop(name: string) {
        return fetch('http://localhost:8059/rest/apis/stop/' + name, {mode: 'no-cors', method:'POST'});
    }

    reloadAll() {
        return fetch('http://localhost:8059/rest/apis/reloadAll', {mode: 'no-cors', method:'POST'});
    }
}