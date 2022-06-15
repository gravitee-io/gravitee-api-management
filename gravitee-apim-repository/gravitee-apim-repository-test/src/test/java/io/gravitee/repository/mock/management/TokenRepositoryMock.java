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
package io.gravitee.repository.mock.management;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TokenRepositoryMock extends AbstractRepositoryMock<TokenRepository> {

    public TokenRepositoryMock() {
        super(TokenRepository.class);
    }

    @Override
    protected void prepare(TokenRepository tokenRepository) throws Exception {
        final Token token = mock(Token.class);
        when(token.getId()).thenReturn("token123");
        when(token.getToken()).thenReturn("token1");
        when(token.getName()).thenReturn("My personal token");
        when(token.getReferenceType()).thenReturn("USER");
        when(token.getReferenceId()).thenReturn("123");
        when(token.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(token.getExpiresAt()).thenReturn(new Date(1486772200000L));
        when(token.getLastUseAt()).thenReturn(new Date(1486773200000L));

        final Token token2 = mock(Token.class);
        when(token2.getId()).thenReturn("token_to_delete");
        when(token2.getToken()).thenReturn("token2");
        when(token2.getName()).thenReturn("My token");
        when(token2.getReferenceType()).thenReturn("USER");
        when(token2.getReferenceId()).thenReturn("123");
        when(token2.getCreatedAt()).thenReturn(new Date(1486771200000L));

        final Token createdToken = mock(Token.class);
        when(createdToken.getId()).thenReturn("new-token");
        when(createdToken.getToken()).thenReturn("token0");
        when(createdToken.getName()).thenReturn("Token name");
        when(createdToken.getReferenceType()).thenReturn("USER");
        when(createdToken.getReferenceId()).thenReturn("456");
        when(createdToken.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(createdToken.getExpiresAt()).thenReturn(new Date(1486772200000L));
        when(createdToken.getLastUseAt()).thenReturn(new Date(1486773200000L));

        final Token updatedToken = mock(Token.class);
        when(updatedToken.getId()).thenReturn("token123");
        when(updatedToken.getToken()).thenReturn("new_token");
        when(updatedToken.getName()).thenReturn("New token name");
        when(updatedToken.getReferenceType()).thenReturn("New ref type");
        when(updatedToken.getReferenceId()).thenReturn("New ref id");
        when(updatedToken.getCreatedAt()).thenReturn(new Date(1486774200000L));
        when(updatedToken.getExpiresAt()).thenReturn(new Date(1486775200000L));
        when(updatedToken.getLastUseAt()).thenReturn(new Date(1486776200000L));

        final List<Token> tokens = asList(token, token2);
        final List<Token> tokensAfterDelete = singletonList(token);

        when(tokenRepository.findAll()).thenReturn(newSet(token, token2, mock(Token.class), mock(Token.class)));

        when(tokenRepository.findByReference("USER", "123")).thenReturn(tokens, tokensAfterDelete, tokens);
        when(tokenRepository.findByReference("USER", "456")).thenReturn(emptyList(), singletonList(createdToken));
        when(tokenRepository.findByReference("USER", "token_to_delete")).thenReturn(singletonList(mock(Token.class)), emptyList());

        when(tokenRepository.create(any(Token.class))).thenReturn(createdToken);

        when(tokenRepository.findById("new-token")).thenReturn(of(createdToken));
        when(tokenRepository.findById("token123")).thenReturn(of(token), of(updatedToken));

        when(tokenRepository.update(argThat(o -> o == null || o.getToken().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
