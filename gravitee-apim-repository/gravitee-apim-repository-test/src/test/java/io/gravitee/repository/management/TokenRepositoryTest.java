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
package io.gravitee.repository.management;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.repository.management.model.Token;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/token-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Token> tokens = tokenRepository.findAll();
        assertNotNull(tokens);
        assertEquals(6, tokens.size());
        final Token token123 = tokens
            .stream()
            .filter(token -> "token123".equals(token.getId()))
            .findAny()
            .get();
        assertToken(token123);
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final List<Token> tokens = tokenRepository.findByReference("USER", "123");
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        final Token token123 = tokens
            .stream()
            .filter(token -> "token123".equals(token.getId()))
            .findAny()
            .get();
        assertToken(token123);
    }

    private void assertToken(Token tokenProduct) {
        assertEquals("My personal token", tokenProduct.getName());
        assertEquals("USER", tokenProduct.getReferenceType());
        assertEquals("123", tokenProduct.getReferenceId());
        assertEquals(new Date(1486771200000L), tokenProduct.getCreatedAt(), "created at");
        assertEquals(new Date(1486772200000L), tokenProduct.getExpiresAt(), "expire at");
        assertEquals(new Date(1486773200000L), tokenProduct.getLastUseAt(), "last use at");
    }

    @Test
    public void shouldCreate() throws Exception {
        final Token token = new Token();
        token.setId("new-token");
        token.setToken("token0");
        token.setName("Token name");
        token.setCreatedAt(new Date(1486771200000L));
        token.setExpiresAt(new Date(1486772200000L));
        token.setLastUseAt(new Date(1486773200000L));
        token.setReferenceType("USER");
        token.setReferenceId("456");

        int nbTokensBeforeCreation = tokenRepository.findByReference("USER", "456").size();
        tokenRepository.create(token);
        int nbTokensAfterCreation = tokenRepository.findByReference("USER", "456").size();

        Assertions.assertEquals(nbTokensBeforeCreation + 1, nbTokensAfterCreation);

        Optional<Token> optional = tokenRepository.findById("new-token");
        Assertions.assertTrue(optional.isPresent(), "Token saved not found");

        final Token tokenSaved = optional.get();
        Assertions.assertEquals(token.getToken(), tokenSaved.getToken(), "Invalid saved token.");
        Assertions.assertEquals(token.getName(), tokenSaved.getName(), "Invalid saved token name.");
        Assertions.assertEquals(token.getReferenceType(), tokenSaved.getReferenceType(), "Invalid token ref type.");
        Assertions.assertEquals(token.getReferenceId(), tokenSaved.getReferenceId(), "Invalid token ref id.");
        Assertions.assertEquals(token.getCreatedAt(), tokenSaved.getCreatedAt(), "Invalid token created date.");
        Assertions.assertEquals(token.getExpiresAt(), tokenSaved.getExpiresAt(), "Invalid token expire date.");
        Assertions.assertEquals(token.getLastUseAt(), tokenSaved.getLastUseAt(), "Invalid token last use date.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Token> optional = tokenRepository.findById("token123");
        Assertions.assertTrue(optional.isPresent(), "Token to update not found");
        Assertions.assertEquals("My personal token", optional.get().getName(), "Invalid saved token name.");

        final Token token = optional.get();
        token.setToken("new_token");
        token.setName("New token name");
        token.setReferenceType("New ref type");
        token.setReferenceId("New ref id");
        token.setCreatedAt(new Date(1486774200000L));
        token.setExpiresAt(new Date(1486775200000L));
        token.setLastUseAt(new Date(1486776200000L));

        int nbTokensBeforeUpdate = tokenRepository.findByReference("USER", "token123").size();
        tokenRepository.update(token);
        int nbTokensAfterUpdate = tokenRepository.findByReference("USER", "token123").size();

        Assertions.assertEquals(nbTokensBeforeUpdate, nbTokensAfterUpdate);

        Optional<Token> optionalUpdated = tokenRepository.findById("token123");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Token to update not found");

        final Token tokenUpdated = optionalUpdated.get();
        Assertions.assertEquals("new_token", tokenUpdated.getToken(), "Invalid saved token.");
        Assertions.assertEquals("New token name", tokenUpdated.getName(), "Invalid saved token name.");
        Assertions.assertEquals("New ref type", tokenUpdated.getReferenceType(), "Invalid token ref type.");
        Assertions.assertEquals("New ref id", tokenUpdated.getReferenceId(), "Invalid token ref id.");
        Assertions.assertEquals(new Date(1486774200000L), tokenUpdated.getCreatedAt(), "Invalid token created date.");
        Assertions.assertEquals(new Date(1486775200000L), tokenUpdated.getExpiresAt(), "Invalid token expire date.");
        Assertions.assertEquals(new Date(1486776200000L), tokenUpdated.getLastUseAt(), "Invalid token expire date.");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTokensBeforeDeletion = tokenRepository.findByReference("USER", "123").size();
        tokenRepository.delete("token_to_delete");
        int nbTokensAfterDeletion = tokenRepository.findByReference("USER", "123").size();

        Assertions.assertEquals(nbTokensBeforeDeletion - 1, nbTokensAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownToken() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Token unknownToken = new Token();
            unknownToken.setToken("unknown");
            tokenRepository.update(unknownToken);
            fail("An unknown token should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            tokenRepository.update(null);
            fail("A null token should not be updated");
        });
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final var nbBeforeDeletion = tokenRepository.findByReference("USER", "ToBeDeleted").size();
        final var deleted = tokenRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", "USER").size();
        final var nbAfterDeletion = tokenRepository.findByReference("USER", "ToBeDeleted").size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted);
        assertEquals(0, nbAfterDeletion);
    }
}
