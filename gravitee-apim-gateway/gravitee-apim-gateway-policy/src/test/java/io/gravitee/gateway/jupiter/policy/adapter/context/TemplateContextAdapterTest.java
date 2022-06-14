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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import static org.mockito.Mockito.*;

import io.gravitee.el.TemplateContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class TemplateContextAdapterTest {

    protected static final String VARIABLE_NAME = "variable";
    protected static final String VARIABLE_VALUE = "value";

    @Mock
    private TemplateContext templateContext;

    private TemplateContextAdapter cut;

    @BeforeEach
    void init() {
        cut = new TemplateContextAdapter(templateContext);
    }

    @Test
    void shouldCallDelegateSetVariable() {
        cut.setVariable(VARIABLE_NAME, VARIABLE_VALUE);
        verify(templateContext).setVariable(VARIABLE_NAME, VARIABLE_VALUE);
    }

    @Test
    void shouldCallDelegateLookupVariable() {
        cut.lookupVariable(VARIABLE_NAME);
        verify(templateContext).lookupVariable(VARIABLE_NAME);
    }

    @Test
    void shouldNotCallDelegateSetDeferredVariable() {
        cut.setDeferredVariable(VARIABLE_NAME, Completable.complete());
        cut.setDeferredVariable(VARIABLE_NAME, Maybe.empty());
        cut.setDeferredVariable(VARIABLE_NAME, Single.error(new RuntimeException()));

        verifyNoInteractions(templateContext);
    }

    @Test
    void shouldBackupAndRestoreWhenExistingVariableIsReplaced() {
        when(templateContext.lookupVariable(VARIABLE_NAME)).thenReturn(VARIABLE_VALUE);

        cut.setVariable(VARIABLE_NAME, "New Value");
        verify(templateContext).setVariable(VARIABLE_NAME, "New Value");

        cut.restore();
        verify(templateContext).setVariable(VARIABLE_NAME, VARIABLE_VALUE);
    }

    @Test
    void shouldNotRestoreAnythingWhenNoExistingVariableHaveBeenReplaced() {
        cut.setVariable(VARIABLE_NAME, "New Value");
        verify(templateContext).setVariable(VARIABLE_NAME, "New Value");

        cut.restore();
        verify(templateContext).lookupVariable(VARIABLE_NAME);
        verifyNoMoreInteractions(templateContext);
    }
}
