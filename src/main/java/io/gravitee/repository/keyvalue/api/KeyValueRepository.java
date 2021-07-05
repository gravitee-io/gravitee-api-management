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
package io.gravitee.repository.keyvalue.api;

import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface KeyValueRepository {

	/**
	 * Get object from store 
	 * 
	 * @param key
	 * @return
	 */
	Object get(String key);
	
	/**
	 * Put a value in store, return previous value if any
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	Object put(String key, Object value);
	
	/**
	 * Put a value in store, return previous value if any
	 * ttl  time to live of the object in second.
	 * 
	 * @param key
	 * @param value
	 * @param ttl
	 * @return
	 */
	Object put(String key, Object value, long ttl);
	
	/**
	 * Put a value in store, return previous value if any
	 * ttl  time to live of the object in ttlUnit.
	 *  
	 * @param key
	 * @param value
	 * @param ttl
	 * @param ttlUnit
	 * @return
	 */
	Object put(String key, Object value, long ttl, TimeUnit ttlUnit);
}
