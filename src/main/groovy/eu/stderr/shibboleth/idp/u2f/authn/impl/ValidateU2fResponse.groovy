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

package eu.stderr.shibboleth.idp.u2f.authn.impl

import com.yubico.u2f.exceptions.U2fBadConfigurationException
import eu.stderr.shibboleth.idp.u2f.authn.DeviceDataStore
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractValidationAction
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import org.opensaml.profile.context.ProfileRequestContext

import javax.annotation.Nonnull
import javax.security.auth.Subject

@Slf4j
class ValidateU2fResponse extends AbstractValidationAction {

    /** Spring injected data store */
    @Nonnull
    @NotEmpty
    private DeviceDataStore dataStore

    /** Inject token data store */
    public void setDeviceDataStore(@Nonnull @NotEmpty final DeviceDataStore tokenData) { this.dataStore = tokenData}

    private U2fUserContext u2fUserContext


    /** Constructor */
    public ValidateU2fResponse() {
        super()
        log.debug("Constructor ValidateU2fResponse()")
    }

    @Override
    protected void doExecute(@Nonnull ProfileRequestContext profileRequestContext,
                             @Nonnull AuthenticationContext authenticationContext) {
        u2fUserContext = authenticationContext.getSubcontext(U2fUserContext.class, true)

        log.debug("{} validating U2F response for user {}", logPrefix, u2fUserContext.username)

        /** Check for errorCode in response */
        def jsonSlurper = new JsonSlurper()
        def tokenResponse = jsonSlurper.parseText(u2fUserContext.tokenResponse)

        if (tokenResponse?.errorCode) {
            log.debug("{} errorCode: ", logPrefix, u2fUserContext.tokenResponse)
            switch (tokenResponse.errorCode) {
                case 2:
                    log.error("{} U2F bad request, most likely mismatch between URL and APP_ID", logPrefix)
                    u2fUserContext.state = AuthnEventIds.REQUEST_UNSUPPORTED
                    throw new U2fBadConfigurationException("URL and APP_ID mismatch")
                    break
                case 4:
                    log.info("{} U2F token does not match {}", logPrefix, u2fUserContext.username)
                    u2fUserContext.state = AuthnEventIds.INVALID_CREDENTIALS
                    handleError(profileRequestContext, authenticationContext, 'InvalidCredentials', AuthnEventIds.INVALID_CREDENTIALS)
                    break
                case 5:
                    log.info("{} U2F token request timeout no input from user: {}", logPrefix, u2fUserContext.username)
                    u2fUserContext.state = AuthnEventIds.NO_CREDENTIALS
                    handleError(profileRequestContext, authenticationContext, 'NoCredentials', AuthnEventIds.NO_CREDENTIALS)
                    break
                default:
                    log.info("{} U2F ", logPrefix)
                    u2fUserContext.state = AuthnEventIds.ACCOUNT_ERROR
                    handleError(profileRequestContext, authenticationContext, 'AccountError', AuthnEventIds.ACCOUNT_ERROR)
                    break
            }
        } else {
            def result = dataStore.finishAuthentication(u2fUserContext)
            if (result) {
                log.info("{} U2F login successful", logPrefix)
                buildAuthenticationResult(profileRequestContext, authenticationContext)
            } else {
                handleError(profileRequestContext, authenticationContext, 'InvalidCredentials', AuthnEventIds.INVALID_CREDENTIALS)
            }
        }
    }

    @Override
    protected Subject populateSubject(@Nonnull Subject subject) {
        subject.principals.add(new U2fPrincipal(u2fUserContext.username))
        return subject
    }
}
