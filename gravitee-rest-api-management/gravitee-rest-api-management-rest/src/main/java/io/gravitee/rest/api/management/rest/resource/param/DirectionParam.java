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
package io.gravitee.rest.api.management.rest.resource.param;

import javax.ws.rs.WebApplicationException;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DirectionParam extends AbstractParam<DirectionParam.Direction> {

    public enum Direction {
        ASC("asc"),
        DESC("desc");

        private final String direction;

        Direction(String direction) {
            this.direction = direction;
        }

        public static Direction fromString(String value) {

            try {
                return Direction.valueOf(value.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(
                        "Invalid value '%s' for directions given! Has to be either 'desc' or 'asc' (case insensitive).", value), e);
            }
        }
    }

    public DirectionParam(String param) throws WebApplicationException {
        super(param);
    }

    @Override
    protected Direction parse(String param) throws Throwable {
        try {
            if (param != null) {
                return DirectionParam.Direction.fromString(param);
            }
        } catch (IllegalArgumentException iae) {
        }

        return Direction.ASC;
    }
}
