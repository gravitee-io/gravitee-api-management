/*
 *
 *  * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.gateway.security.core;

import io.gravitee.common.security.CertificateUtils;
import io.gravitee.gateway.api.service.Subscription;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * <p>A record that encapsulates information about a subscription certificate,
 * including the associated subscription, certificate, and certificate fingerprint.</p>
 *
 * <p>This record ensures that all fields are non-null through its constructor validation
 * and provides additional methods for equality and hashing based on the subscription ID
 * and fingerprint.</p>
 *
 * <p>The SubscriptionCertificate is typically used to bind a subscription to its corresponding
 * security certificate along with a unique fingerprint representation of the certificate.</p>
 *
 * <h3>Components:</h3>
 * <ul>
 * <li><code>subscription</code>: The subscription associated with the certificate.</li>
 * <li><code>certificate</code>: The security certificate associated with the subscription.</li>
 * <li><code>fingerprint</code>: A unique hash representation (fingerprint) of the certificate.</li>
 * </ul>
 *
 * <h3>Additional Constructor:</h3>
 * <p>Includes a secondary constructor that generates a SHA-256 fingerprint automatically
 * from the given certificate using the {@code CertificateUtils.generateThumbprint} method.</p>
 *
 * <h3>Overrides:</h3>
 * <ul>
 * <li><code>hashCode</code>: Computes a hash code based on the subscription ID and fingerprint.</li>
 * <li><code>equals</code>: Provides equality comparison based on the subscription ID and fingerprint.</li>
 * <li><code>toString</code>: Provides a string representation including the subscription ID and fingerprint.</li>
 * </ul>
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SubscriptionCertificate(Subscription subscription, Certificate certificate, String fingerprint) {
    /**
     * Constructs a {@code SubscriptionCertificate} record instance with the specified
     * subscription, certificate, and fingerprint. Ensures that none of the input parameters are null.
     *
     * @param subscription The subscription associated with the certificate. Must not be null.
     * @param certificate  The security certificate associated with the subscription. Must not be null.
     * @param fingerprint  A unique hash representation (fingerprint) of the certificate. Must not be null.
     * @throws NullPointerException if any of the provided parameters are null.
     */
    public SubscriptionCertificate {
        Objects.requireNonNull(subscription, "Subscription must not be null");
        Objects.requireNonNull(certificate, "Certificate must not be null");
        Objects.requireNonNull(fingerprint, "Fingerprint must not be null");
    }

    /**
     * Constructs a {@code SubscriptionCertificate} record instance using the specified
     * subscription and certificate. Automatically generates the certificate fingerprint
     * using the SHA-256 algorithm.
     *
     * @param subscription The subscription associated with the certificate. Must not be null.
     * @param certificate  The security certificate associated with the subscription. Must not be null.
     * @throws NullPointerException if the subscription or certificate is null.
     */
    public SubscriptionCertificate(Subscription subscription, Certificate certificate) {
        this(subscription, certificate, CertificateUtils.generateThumbprint((X509Certificate) certificate, "SHA-256"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscription.getId(), fingerprint);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SubscriptionCertificate that = (SubscriptionCertificate) obj;
        return Objects.equals(subscription.getId(), that.subscription.getId()) && Objects.equals(fingerprint, that.fingerprint);
    }

    @Override
    public @NonNull String toString() {
        return "SubscriptionCertificate{" + "subscription=" + subscription.getId() + ", fingerprint='" + fingerprint + '\'' + '}';
    }
}
