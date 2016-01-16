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
import com.yubico.u2f.U2F
import com.yubico.u2f.data.DeviceRegistration
import com.yubico.u2f.data.messages.AuthenticateRequestData
import com.yubico.u2f.data.messages.AuthenticateResponse
import com.yubico.u2f.exceptions.DeviceCompromisedException
import eu.stderr.shibboleth.idp.u2f.authn.DeviceDataStore
import eu.stderr.shibboleth.idp.u2f.authn.impl.U2fUserContext
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

@Slf4j
public class DummyDataStore implements DeviceDataStore {
    private def keyHandle
    private def publicKey
    private def counter = 0
    private def compromised = false
    final private U2F u2f = new U2F()

    /** Constructor */
    public DummyDataStore(String keyHandle, String publicKey) {
        log.debug("U2F DummyDataStore constructor")
        this.publicKey = publicKey
        this.keyHandle = keyHandle
    }

    @Override
    def beginAuthentication(String username, U2fUserContext u2fUserContext) {
        List<DeviceRegistration> tokens = new ArrayList<DeviceRegistration>()
        if (!u2fUserContext?.tokens) {
            def json = JsonOutput.toJson([keyHandle: keyHandle, publicKey: publicKey,
                                          counter: counter, compromised: compromised])
            tokens.add(DeviceRegistration.fromJson(json))
            u2fUserContext.tokens = tokens
        }
        if (!u2fUserContext?.authenticateRequestData) {
            u2fUserContext.authenticateRequestData = u2f.startAuthentication(u2fUserContext.appId as String, tokens)
        }
    }

    @Override
    boolean finishAuthentication(U2fUserContext u2fUserContext) {
        def username = u2fUserContext.username
        AuthenticateResponse authenticateResponse = AuthenticateResponse.fromJson(u2fUserContext.tokenResponse)
        log.debug("Signature data for {}: {}", username, authenticateResponse.signatureData)
        DeviceRegistration device
        try {
            device = u2f.finishAuthentication(u2fUserContext.authenticateRequestData as AuthenticateRequestData,
                    authenticateResponse, u2fUserContext.tokens as List<DeviceRegistration>)
            if (counter >= device.counter) {
                log.error("Device {} for user {} possibly compromised, device counter mismatch",
                        device.keyHandle, username)
                throw new DeviceCompromisedException(device, "Device counter mismatch, possibly a cloned token.")
            }
            return true //Authentication successful
        } catch (DeviceCompromisedException e) {
            DeviceRegistration registration = e.getDeviceRegistration()
            log.error("Device {} for user {} marked as compromised, authentication blocked", registration.keyHandle,
            username)
            throw new DeviceCompromisedException(registration, e.getMessage())
        }
    }
}
