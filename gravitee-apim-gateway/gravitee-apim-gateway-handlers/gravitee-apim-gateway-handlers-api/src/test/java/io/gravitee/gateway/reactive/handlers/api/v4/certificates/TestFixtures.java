/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.gateway.reactive.handlers.api.v4.certificates;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class TestFixtures {

    public static final String CERT =
        """
                        -----BEGIN CERTIFICATE-----
                        MIIDazCCAlOgAwIBAgIUJjfny3beplZzojjkJ1fhbV1RHD4wDQYJKoZIhvcNAQEL
                        BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
                        GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMzA5MDgxMDM3MzVaFw0yNDA4
                        MjkxMDM3MzVaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
                        HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
                        AQUAA4IBDwAwggEKAoIBAQCt3A4qP+9Rl7iv/wx3fi33sVECYJBTpUMouDl9Amu2
                        Gi/W5nsbRQY26KenWPr05wrnDlDvsnLxRXbb3ezdwcbFbT8m7Qvec0jId0XhU40m
                        b0DUjCs4vQCyAKde/VpJC0soNsc0Wfx9NWAEdRvwfdJJdQ+v75tO2SzuiK460dFo
                        rOtwVwLKL3KOD0syifUHEKeDJS6eN3h/N1nM6wI8jnpXoHgN8RJ/2G7SZPyn1rmY
                        lEjoX57daAVEtR011nHO97zdncBjfR/iswsfmkhCisbKi5P+Lng9OS3RF5dl30wG
                        8tiHIOAn2z0eAQNoyr70oLtCaHjC+SPPuzwAps1gfUf1AgMBAAGjUzBRMB0GA1Ud
                        DgQWBBQ3syOvxPbQq4GaYFTjP7EantnBzzAfBgNVHSMEGDAWgBQ3syOvxPbQq4Ga
                        YFTjP7EantnBzzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQB1
                        ws1gimBdXMJ00IgrzyZd6nS9roGAbIueWEnnfVsJAuz1kc1WtGzZPDPW7qUHoZNy
                        Lcb/xksIsw8MnFhmC++aiB4c+VmNNeqdY+pHVFhgEuCsH/Mm/Obkvw1zImfOmurp
                        QZXEdTZ6uQVYPYZ8kyfABJg5bkCWKc++XbtsFQy2H4Xk8tYvABLKrxh3mkkgTypx
                        dxDgjT806ZVjxgXdcryMskFX8amsofowzDwU6u8Wo+SW8jloItWv+j5hCR8eiIIz
                        29AxHtIJmaiTidz2eHsjfuhSqKgS74ndeJnsdz5ZHRsWoEtu0t/nIrwSclZKrjBq
                        VXwOSZSQT3z99f/MsavL
                        -----END CERTIFICATE-----
                        """;
    public static final String KEY =
        """
                                            -----BEGIN PRIVATE KEY-----
                                            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCt3A4qP+9Rl7iv
                                            /wx3fi33sVECYJBTpUMouDl9Amu2Gi/W5nsbRQY26KenWPr05wrnDlDvsnLxRXbb
                                            3ezdwcbFbT8m7Qvec0jId0XhU40mb0DUjCs4vQCyAKde/VpJC0soNsc0Wfx9NWAE
                                            dRvwfdJJdQ+v75tO2SzuiK460dForOtwVwLKL3KOD0syifUHEKeDJS6eN3h/N1nM
                                            6wI8jnpXoHgN8RJ/2G7SZPyn1rmYlEjoX57daAVEtR011nHO97zdncBjfR/iswsf
                                            mkhCisbKi5P+Lng9OS3RF5dl30wG8tiHIOAn2z0eAQNoyr70oLtCaHjC+SPPuzwA
                                            ps1gfUf1AgMBAAECggEADhqWaZYDL47L1DcwBzeMuhW/2R4FR0vWTWTYgQwjucOZ
                                            Eulinj00ulqYUyqUPS7LAyB1r2Q+D9WPRVnU/85a9iQdJea/+j1G78BBQny5LB+F
                                            VljCntkyR75m1X1fCCLq52m+MkCEi5G7ZtErQZCrcPsWmTKqWjSjAPzEiZAA2Wlf
                                            Z3hemgge3pmASz964TR4Nd1yC6rceEJvAr5d/Ez6MU8mgez9o/ZuaIoi0q4n12NZ
                                            /rexM9B8rnP93nedNjyy1lCc9+T8x0s7haN/ZjKR3nGj+cp6PCxAgNX18G5shmqR
                                            6bJrjn0Mu04w2n3bfoG0NNNpf3j06vIP1HNyAuhKlwKBgQDhtzfet3/h68eDJO3m
                                            oD3oI45vDvesHgIeXPR+BZGsujW6ab1DSUEeZhAgbxooD/NioWZVer/jehgcvJdg
                                            TUALq63so4Q24DFJp6WdQPU0uLvlajqhykF0SccdFo8iN3xGGbCK8Kb2tHexULaN
                                            rvPCLZTEjlpPzULUemc70yAVowKBgQDFL7TwMakwiTk4ed26uoru1cth+IOQz1YP
                                            DoiGvBTU0uvegGCclWxFwkfXfMzqQGpTK2v9EG2afL5CZUnGCSAO2Zq6nTuXpLr4
                                            GmtosQcJmzA7BDiY86eLDsSCxAQb/5xOqjDIvJR/BZnH7+8duqCWcMqiwYoUdz1n
                                            qxJCZb6VhwKBgBwI8buL9ypMar9zOslGZeoLYImSxlhucbzrtsJgVrOpfTrmH0fY
                                            NWpdKuucYRdQw94gReGgGW1boNsQ4Yxoi+fnLvcRaD6YogaP+BYMF2iw+UWJaDbo
                                            NDEJaN3IC4codRsP3cmkEljaGXPAnqwCauxXVP8E31rCF+bkPSZFFtsZAoGAV1CU
                                            sneLD67z44ozIOhRdQi+kpdUyt7EoM4yrlbCcqsjPtdh8HRKCWnKHiVpJ6F2c3Wa
                                            z+hiYDI0nXn0fPi1dV3uIgxVwwRytkIcpbMeBqbtaHSqCzB5VB4p7i2WFD/PmxXJ
                                            nFnE96onOl2IaIWnbnZrhD5nQkC6tBkQcM5U4ikCgYAUMBYsZJpTnPYojMp6EM9B
                                            icwZQsuhNFgn+WM2/itFlPH7N/s1cScs4stkS1OzrlzZHLAzOfbqLeTbpNfQM5lE
                                            utWjVUNvzathT7PMDCxR1VtuNvpAZon5/ResDgimGyr/YvZ5XuriHdudTeAN75TZ
                                            0LCyEgd6Noz/STJZdPuW+A==
                                            -----END PRIVATE KEY-----
                                            """;
}
