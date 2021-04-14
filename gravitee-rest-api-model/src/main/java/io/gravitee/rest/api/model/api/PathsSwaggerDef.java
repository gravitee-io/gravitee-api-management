/**
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
package io.gravitee.rest.api.model.api;

import io.gravitee.definition.model.Rule;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * /!\ This class shouldn't be used.
 * It was created to fix the issue https://github.com/gravitee-io/issues/issues/4406
 * and allow a correct swagger generation without modifying the ApiEntity/UpdateApiEntity structure
 *
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
class PathsSwaggerDef extends HashMap<String, ArrayList<Rule>> {}
