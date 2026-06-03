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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.ApplicationInvitationAction;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.GroupInvitationAction;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.UserRegistrationAction;
import io.gravitee.apim.core.user.model.DecodedToken;
import io.gravitee.apim.core.user.model.RawPassword;
import io.gravitee.rest.api.portal.rest.model.FinalizeRegistrationInput;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FinalizeRegistrationMapper {

    public AcceptUserInvitationUseCase.Input toUseCaseInput(
        ExecutionContext executionContext,
        DecodedToken decoded,
        FinalizeRegistrationInput input
    ) {
        var action = switch (decoded.action()) {
            case String s when JWTHelper.ACTION.GROUP_INVITATION.name().equals(s) -> new GroupInvitationAction(
                decoded.email(),
                decoded.subject()
            );
            case String s when JWTHelper.ACTION.APPLICATION_INVITATION.name().equals(s) -> new ApplicationInvitationAction(
                decoded.email(),
                decoded.subject()
            );
            default -> new UserRegistrationAction(decoded.email(), decoded.subject());
        };

        return new AcceptUserInvitationUseCase.Input(
            executionContext,
            action,
            Optional.ofNullable(input.getPassword()).map(RawPassword::new),
            Optional.ofNullable(input.getFirstname()),
            Optional.ofNullable(input.getLastname())
        );
    }
}
