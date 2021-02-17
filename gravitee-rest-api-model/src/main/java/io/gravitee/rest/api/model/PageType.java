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
package io.gravitee.rest.api.model;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * @author Ludovic Dussart (ludovic.dussart at gmail.com)
 * @author Guillaume GILLON
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * Managed types for page documentation
 *
 */
public enum PageType {

	MARKDOWN(unmodifiableList(asList("md", "markdown")), 200),
	MARKDOWN_TEMPLATE(emptyList(), 200),
	SWAGGER(unmodifiableList(asList("json", "yaml", "yml")), 200),
	FOLDER(emptyList(), 300),
    LINK(emptyList(), 100),
    ROOT(emptyList(), 500),
    SYSTEM_FOLDER(emptyList(), 400),
    TRANSLATION(emptyList(), 0);

	List<String> extensions;
	Integer removeOrder;

	PageType(List<String> extensions, Integer removeOrder) {
		this.extensions = extensions;
		this.removeOrder = removeOrder;
	}

	public List<String> extensions() {
		return extensions;
	}

	public Integer getRemoveOrder() {
		return removeOrder;
	}
}
