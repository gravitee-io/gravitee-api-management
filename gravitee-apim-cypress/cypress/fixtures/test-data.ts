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
import * as faker from "faker";
import {Api} from "./apis";

const username = "api1";
const password = "api1";

export const fakeUserData = {
    username,
    password,
};

export function version() {
    const major = faker.datatype.number({min: 1, max: 5});
    const minor = faker.datatype.number({min: 1, max: 10});
    const patch = faker.datatype.number({min: 1, max: 30});
    return `${major}.${minor}.${patch}`;
}

export function createFakeAPI(attributes?: any) {
    const name = faker.commerce.productName();
    return {
        ...attributes,
        contextPath: `/${faker.random.word()}-${faker.datatype.uuid()}`,
        name,
        description: faker.commerce.productDescription(),
        version: version(),
        endpoint: "https://api.gravitee.io/echo",
    } ;
}
