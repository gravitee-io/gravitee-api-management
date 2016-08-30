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
package io.gravitee.repository.cache.model;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Element {

	static Element from(final Object key, final Object value) {
		return new Element() {
			@Override
			public Object key() {
				return key;
			}

			@Override
			public Object value() {
				return value;
			}
		};
	}

	static Element from(final Object key, final Object value, final int timeToLive) {
		return new Element() {
			@Override
			public Object key() {
				return key;
			}

			@Override
			public Object value() {
				return value;
			}

			@Override
			public int timeToLive() {
				return timeToLive;
			}
		};
	}

	Object key();

	Object value();

	default int timeToLive() {
		return 0;
	}
}
