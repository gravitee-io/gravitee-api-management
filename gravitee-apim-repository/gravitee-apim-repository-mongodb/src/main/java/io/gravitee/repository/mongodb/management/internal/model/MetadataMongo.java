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
package io.gravitee.repository.mongodb.management.internal.model;

import static org.springframework.data.mongodb.core.EncryptionAlgorithms.AEAD_AES_256_CBC_HMAC_SHA_512_Deterministic;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.ExplicitEncrypted;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode(of = { "id" }, callSuper = false)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}metadata")
public class MetadataMongo extends DeprecatedAuditable {

    @Id
    private MetadataPkMongo id;

    private String name;

    private String format;

    @ExplicitEncrypted(algorithm = AEAD_AES_256_CBC_HMAC_SHA_512_Deterministic)
    private String value;
}
