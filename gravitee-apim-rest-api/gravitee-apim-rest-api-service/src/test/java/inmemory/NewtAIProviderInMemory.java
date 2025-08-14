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
package inmemory;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.newtai.model.ELGenQuery;
import io.gravitee.apim.core.newtai.model.ELGenReply;
import io.gravitee.apim.core.newtai.service_provider.NewtAIProvider;
import io.reactivex.rxjava3.core.Single;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NewtAIProviderInMemory implements NewtAIProvider, InMemoryAlternative<NewtAIProviderInMemory.Tuple> {

    private final Map<String, Tuple> storage = new HashMap<>();

    @Override
    public Single<ELGenReply> generateEL(ELGenQuery query) {
        return switch (storage.get(query.message())) {
            case Tuple.Fail fail -> Single.error(fail.reply());
            case Tuple.Success success -> Single.just(success.reply());
        };
    }

    @Override
    public void initWith(List<Tuple> items) {
        reset();
        storage.putAll(stream(items).collect(Collectors.toMap(Tuple::inputMessage, Function.identity())));
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Tuple> storage() {
        return List.copyOf(storage.values());
    }

    public sealed interface Tuple {
        String inputMessage();

        record Success(String inputMessage, ELGenReply reply) implements NewtAIProviderInMemory.Tuple {}

        record Fail(String inputMessage, Throwable reply) implements NewtAIProviderInMemory.Tuple {}
    }
}
