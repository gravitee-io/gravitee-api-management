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
package io.gravitee.repository.mongodb.encryption;

import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClientException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import io.gravitee.repository.mongodb.common.MongoFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.convert.encryption.MongoEncryptionConverter;
import org.springframework.data.mongodb.core.encryption.Encryption;
import org.springframework.data.mongodb.core.encryption.EncryptionKey;
import org.springframework.data.mongodb.core.encryption.EncryptionKeyResolver;
import org.springframework.data.mongodb.core.encryption.MongoClientEncryption;

/**
 *  This Configuration class provides Beans for encryption. They are activated when the environment variable <code>management.mongodb.encryption.enabled</code> is <b>true</b>. <br/>
 *  For the moment, we support only local KMS configuration. But we could imagine to support more KMS provider. <br/>
 *  That's why we have designed the configuration part of encryption like this but this may change in the future:
 *  <pre>
 *     mongodb:
 *       ...
 *       ## Encryption configuration
 *       encryption:
 *         enabled: true
 *         kms:
 *           - type: local                           # local, azure, aws, gcp. Only local is supported at the moment.
 *             local:
 *               key: secret://kubernetes/.....
 *         keyVault:
 *           keyAlternativeName: cloud-repository
 *           collectionName: __dataKeys
 *     #      databaseName: encryption               # Not yet supported. Could be used in next versions of APIM
 *  </pre>
 */
@Configuration
@Slf4j
public class EncryptionConfiguration {

    private static final String KEY_MANAGEMENT_SYSTEM_PROPERTY = "management.mongodb.encryption.kms";
    private static final String KEY_MANAGEMENT_SYSTEM_LOCAL_TYPE = "local";
    private static final String KEY_MANAGEMENT_SYSTEM_LOCAL_KEY_PROPERTY = "key";

    @Value("${management.mongodb.encryption.keyVault.keyAlternativeName:apim-master-key}")
    private String keyAlternativeName;

    @Value("${management.mongodb.encryption.keyVault.collectionName:__dataKeys}")
    private String keyVaultCollectionName;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    private final Environment environment;

    public EncryptionConfiguration(Environment environment) {
        this.environment = environment;
    }

    private Map<String, Map<String, Object>> loadKmsProvidersFromConfig() {
        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty(KEY_MANAGEMENT_SYSTEM_PROPERTY + "[" + idx + "].type");
            found = (type != null);
            if (found) {
                switch (type) {
                    case KEY_MANAGEMENT_SYSTEM_LOCAL_TYPE:
                        kmsProviders.put(type, loadLocalKmsProviderFromConfig(idx));
                        break;
                    default:
                        log.warn("Unsupported key management system type: {}", type);
                        break;
                }
            }
            idx++;
        }
        if (kmsProviders.isEmpty()) {
            throw new IllegalStateException("No key management system provider found");
        }
        return kmsProviders;
    }

    private Map<String, Object> loadLocalKmsProviderFromConfig(int idx) {
        Map<String, Object> localKmsProvider = new HashMap<>();
        String masterKey = environment.getProperty(KEY_MANAGEMENT_SYSTEM_PROPERTY + "[" + idx + "].local.key");
        if (masterKey == null) {
            throw new IllegalStateException("Master key not set");
        }
        localKmsProvider.put(KEY_MANAGEMENT_SYSTEM_LOCAL_KEY_PROPERTY, masterKey);

        return localKmsProvider;
    }

    private String getDatabaseName() {
        String uri = environment.getProperty("management.mongodb.uri");
        if (uri != null && !uri.isEmpty()) {
            // Remove user:password from the URI as it can contain special characters and isn't needed for the database name
            String uriWithoutCredentials = uri.replaceAll("://.*@", "://");
            return URI.create(uriWithoutCredentials).getPath().substring(1);
        }

        return environment.getProperty("management.mongodb.dbname", "gravitee");
    }

    @Bean
    @Conditional(EncryptionEnabledCondition.class)
    public ClientEncryption clientEncryption(MongoFactory mongoFactory) {
        // Key Management System (KMS) providers.
        Map<String, Map<String, Object>> kmsProviders = loadKmsProvidersFromConfig();

        ClientEncryptionSettings.Builder builder = ClientEncryptionSettings
            .builder()
            // The collection in MongoDB where the Data Encryption Keys (DEKs) will be stored.
            .keyVaultNamespace(getDatabaseName() + "." + tablePrefix + keyVaultCollectionName)
            .keyVaultMongoClientSettings(mongoFactory.buildMongoClientSettings(false))
            .kmsProviders(kmsProviders);

        ClientEncryption clientEncryption = ClientEncryptions.create(builder.build());

        log.info("MongoDB encryption enabled");

        // Initialize the Data Encryption Key if not already existing.
        initDataEncryptionKey(clientEncryption);

        return clientEncryption;
    }

    private void initDataEncryptionKey(ClientEncryption clientEncryption) {
        BsonDocument keyByAltName = clientEncryption.getKeyByAltName(keyAlternativeName);
        if (keyByAltName == null) {
            try {
                clientEncryption.createDataKey(
                    KEY_MANAGEMENT_SYSTEM_LOCAL_TYPE,
                    new DataKeyOptions().keyAltNames(List.of(keyAlternativeName))
                );
            } catch (MongoWriteException mwe) {
                if (ErrorCategory.fromErrorCode(mwe.getError().getCode()) == ErrorCategory.DUPLICATE_KEY) {
                    log.debug("KeyAlternativeName has already been created: {}", keyAlternativeName);
                } else {
                    throw mwe;
                }
            }
        }
    }

    @Bean
    @Conditional(EncryptionEnabledCondition.class)
    public MongoEncryptionConverter encryptingConverter(ClientEncryption clientEncryption) {
        Encryption<BsonValue, BsonBinary> encryption = MongoClientEncryption.just(clientEncryption);
        EncryptionKeyResolver keyResolver = EncryptionKeyResolver.annotated(ctx -> EncryptionKey.keyAltName(keyAlternativeName));

        return new MongoEncryptionConverter(encryption, keyResolver);
    }
}
