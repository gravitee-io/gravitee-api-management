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

declare var $;

export class ApisService {

    constructor() {
    }

    list() {
        return $.get("http://localhost:8082/api/apis",
            function(data) {return data;}
        );
    }

    start(name: string) {
        return $.post('http://localhost:8082/api/apis/start/' + name);
    }

    stop(name: string) {
        return $.post('http://localhost:8082/api/apis/stop/' + name);
    }

    reload(name: string) {
        return $.post('http://localhost:8082/api/apis/reload/' + name);
    }
}