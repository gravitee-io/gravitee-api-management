/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.security.filter.error;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorHelper {

    private static final GraviteeMapper GRAVITEE_MAPPER = new GraviteeMapper();

    public static void sendError(final HttpServletResponse httpServletResponse, final int errorCode, final String errorMsg)
        throws IOException {
        httpServletResponse.setStatus(errorCode);
        httpServletResponse
            .getOutputStream()
            .print(GRAVITEE_MAPPER.writeValueAsString(Error.builder().httpStatus(errorCode).message(errorMsg).build()));
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
