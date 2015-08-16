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
package io.gravitee.repositories.mongodb.internal.model;

import java.util.Date;
import java.util.Objects;

/**
 * Mongo model for Api
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 */
public class ApiKeyMongo {
  
	/**
	 * Api Key
	 */
	private String key;

	/**
	 * Is the key revoked ?
	 */
	private boolean revoked;
	
	/**
	 * Token expiration date
	 */
	private Date expiration;

	/**
	 * The API key creation date
	 */
	private Date createdAt;
	
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}
	
	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyMongo key = (ApiKeyMongo) o;
        return Objects.equals(this.key, key.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiKeyMongo{");
        sb.append("name='").append(key).append('\'');
        sb.append(", expiration=").append(expiration );
        sb.append('}');
        return sb.toString();
    }
}
