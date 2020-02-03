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

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewPkMongo implements Serializable {

    private static final long serialVersionUID = -2005761443664662458L;

    private String id;
	private String environment;

	public ViewPkMongo() {
	}

	public ViewPkMongo(String id, String environment) {
	    this.id = id;
	    this.environment = environment;
	}

	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ViewPkMongo)) return false;
		ViewPkMongo that = (ViewPkMongo) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(environment, that.environment);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, environment);
	}

	@Override
	public String toString() {
		return "ViewPkMongo{" +
				"id='" + id + '\'' +
				", environment='" + environment + '\'' +
				'}';
	}
}
