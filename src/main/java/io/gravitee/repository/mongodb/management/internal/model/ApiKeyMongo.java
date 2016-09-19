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
package io.gravitee.repository.mongodb.management.internal.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Objects;

/**
 * Mongo model for Api Key
 *
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "keys")
public class ApiKeyMongo {

	/**
	 * Api Key
	 */
	@Id
	private String key;

	/**
	 * The subscription for which the Api Key is generated
	 */
	private String subscription;

	/**
	 * The application used to make the subscription
	 */
	private String application;

	/**
	 * The subscribed plan
	 */
	private String plan;

	/**
	 * Expiration date (end date) of the Api Key
	 */
	private Date expireAt;

	/**
	 * API key creation date
	 */
	private Date createdAt;

	/**
	 * API key updated date
	 */
	private Date updatedAt;

	/**
	 * Flag to indicate if the Api Key is revoked ?
	 */
	private boolean revoked;

	/**
	 * If the key is revoked, the revocation date
	 */
	private Date revokedAt;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSubscription() {
		return subscription;
	}

	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public Date getExpireAt() {
		return expireAt;
	}

	public void setExpireAt(Date expireAt) {
		this.expireAt = expireAt;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	public Date getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Date revokedAt) {
		this.revokedAt = revokedAt;
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
        final StringBuilder sb = new StringBuilder("ApiKey{");
        sb.append("name='").append(key).append('\'');
        sb.append(", expiration=").append(expireAt).append('\'');
		sb.append(", revoked=").append(revoked).append('\'');
		sb.append(", revokedAt=").append(revokedAt);
        sb.append('}');
        return sb.toString();
    }
}
