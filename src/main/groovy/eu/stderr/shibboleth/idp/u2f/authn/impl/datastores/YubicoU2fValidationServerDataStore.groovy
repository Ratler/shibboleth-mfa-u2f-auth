/*
 * Copyright 2016 Stefan Wold <ratler@stderr.eu>
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
 *
 */

package eu.stderr.shibboleth.idp.u2f.authn.impl.datastores
import eu.stderr.shibboleth.idp.u2f.authn.DeviceDataStore
import eu.stderr.shibboleth.idp.u2f.authn.impl.U2fUserContext
import groovy.util.logging.Slf4j
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.InterceptingClientHttpRequestFactory
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Slf4j
class YubicoU2fValidationServerDataStore extends RestTemplate implements DeviceDataStore {
    private String endPoint
    private HttpHeaders headers = new HttpHeaders()

    /** Constructor */
    YubicoU2fValidationServerDataStore(String endPoint, String username = null, String password = null) {
        log.debug("YubicoU2fValidationServerDataStore constructor adding endpoint (${endPoint})")
        if (endPoint[-1] != '/') {
            endPoint += '/'
        }
        this.endPoint = endPoint

        if (username && password) {
            List<ClientHttpRequestInterceptor> interceptors = Collections
                    .<ClientHttpRequestInterceptor> singletonList(
                    new BasicAuthInterceptor(username, password))
            setRequestFactory(new InterceptingClientHttpRequestFactory(getRequestFactory(), interceptors))
        }
        headers.setContentType(MediaType.APPLICATION_JSON)
    }

    @Override
    def beginAuthentication(String username, U2fUserContext u2fUserContext) {
        HttpEntity<Object> entity = new HttpEntity<>(headers)
        ResponseEntity<String> response = exchange(endPoint + username + '/authenticate', HttpMethod.GET, entity, String.class)
        u2fUserContext.authenticateRequestData = response.body
        log.debug("beginAuthentication() - status code: {} result: {}", response.statusCode, response.body)
    }

    @Override
    boolean finishAuthentication(U2fUserContext u2fUserContext) {
        def username = u2fUserContext.username
        def payload = '{"authenticateResponse": ' + (String) u2fUserContext.tokenResponse + '}'
        log.debug("finishAuthentication() payload: " + payload)
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers)
        try {
            ResponseEntity<String> response = exchange(endPoint + username + '/authenticate', HttpMethod.POST, entity, String.class)
            log.debug("finishAuthentication() status code: ${response.statusCode} payload: ${response.body}")
            return true
        } catch (HttpStatusCodeException e) {
            log.error("U2fval response error: {} {}", e.statusCode, e.responseBodyAsString)
        }
        return false
    }

    /**
     * HTTP basic authentication interceptor for RestTemplate
     */
    private static class BasicAuthInterceptor implements ClientHttpRequestInterceptor {
        private def username
        private def password

        /** Constructor */
        public BasicAuthInterceptor(String username, String password) {
            this.username = username
            this.password = password
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] data, ClientHttpRequestExecution execution) throws IOException {
            byte[] token = Base64.encoder.encode(("${this.username}:${this.password}").getBytes())
            request.headers.add("Authorization", "Basic " + new String(token))
            return execution.execute(request, data)
        }
    }
}
