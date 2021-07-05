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

import java.util.List;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "rating")
public class RatingMongo extends Auditable {

	@Id
	private String id;

    private String referenceId;
    
    private String referenceType;
	
    private String user;

	private byte rate;

	private String title;

	private String comment;

	private List<RatingAnswerMongo> answers;


	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public byte getRate() {
		return rate;
	}

	public void setRate(byte rate) {
		this.rate = rate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public List<RatingAnswerMongo> getAnswers() {
		return answers;
	}

	public void setAnswers(List<RatingAnswerMongo> answers) {
		this.answers = answers;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RatingMongo)) return false;
		RatingMongo that = (RatingMongo) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RatingMongo{" +
				"id='" + id + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
				", user='" + user + '\'' +
				", rate=" + rate +
				", title='" + title + '\'' +
				", comment='" + comment + '\'' +
				", answers=" + answers +
				"} " + super.toString();
	}
}
