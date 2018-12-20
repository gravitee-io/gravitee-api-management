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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKey {
	public enum AuditEvent implements Audit.AuditEvent {
		APIKEY_CREATED, APIKEY_RENEWED, APIKEY_REVOKED, APIKEY_EXPIRED
	}

	/**
	 * Api Key
	 */
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
	 * Flag to indicate if the Api Key is paused ?
	 */
	private boolean paused;

	/**
	 * If the key is revoked, the revocation date
	 */
	private Date revokedAt;

	public ApiKey() {}

	public ApiKey(ApiKey cloned) {
		this.key = cloned.key;
		this.subscription = cloned.subscription;
		this.application = cloned.application;
		this.plan = cloned.plan;
		this.expireAt = cloned.expireAt;
		this.createdAt = cloned.createdAt;
		this.updatedAt = cloned.updatedAt;
		this.revoked = cloned.revoked;
		this.revokedAt = cloned.revokedAt;
		this.paused = cloned.paused;
	}
	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
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

	public Date getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Date revokedAt) {
		this.revokedAt = revokedAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getSubscription() {
		return subscription;
	}

	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ApiKey apiKey = (ApiKey) o;
		return Objects.equals(key, apiKey.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key);
	}
}