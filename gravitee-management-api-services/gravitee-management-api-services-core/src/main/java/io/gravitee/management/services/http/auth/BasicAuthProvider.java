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
package io.gravitee.management.services.http.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BasicAuthProvider implements AuthProvider {

    private final static String USERS_PREFIX_KEY = "services.core.http.authentication.users.";

    @Autowired
    private Environment environment;

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
        String password = environment.getProperty(USERS_PREFIX_KEY + authInfo.getString("username"));

        if (password != null) {
            // Get password from incoming HTTP request
            String presentedPassword = authInfo.getString("password");

            if (password.equals(presentedPassword)) {
                resultHandler.handle(
                        Future.succeededFuture(new io.gravitee.management.services.http.auth.User()));
                return ;
            }
        }

        resultHandler.handle(Future.failedFuture("Unauthorized user"));
    }
}
