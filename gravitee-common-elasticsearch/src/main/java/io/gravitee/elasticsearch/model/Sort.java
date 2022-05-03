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
package io.gravitee.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * Information about the sort value when the elasticsearch query contains
 * a order clause. 
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Sort implements Serializable {
	
	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Double value.
	 */
	private Double d;
	
	/**
	 * String value.
	 */
	private String s;
	
	/**
	 * Integer value.
	 */
	private Integer i;
	
	/**
	 * Long value.
	 */
	private Long l;
	
	/**
	 * Float value.
	 */
	private Float f;

	/**
	 * Constructor with the double value
	 * @param d double value
	 */
	public Sort(Double d) {
		this.d = d;
	}

	/**
	 * Constructor with the string value.
	 * @param s string value.
	 */
	public Sort(String s) {
		this.s = s;
	}

	/**
	 * Constructor with the integer value.
	 * @param i integer value.
	 */
	public Sort(Integer i) {
		this.i = i;
	}

	/**
	 * Constructor with the long value
	 * @param l long value.
	 */
	public Sort(Long l) {
		this.l = l;
	}

	/**
	 * Constructor with the float value.
	 * @param f float value.
	 */
	public Sort(Float f) {
		this.f = f;
	}

    public Double getD() {
        return d;
    }

    public void setD(Double d) {
        this.d = d;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public Integer getI() {
        return i;
    }

    public void setI(Integer i) {
        this.i = i;
    }

    public Long getL() {
        return l;
    }

    public void setL(Long l) {
        this.l = l;
    }

    public Float getF() {
        return f;
    }

    public void setF(Float f) {
        this.f = f;
    }
}
