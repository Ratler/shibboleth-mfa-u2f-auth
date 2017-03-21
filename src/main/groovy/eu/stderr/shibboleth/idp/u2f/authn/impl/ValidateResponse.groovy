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
import eu.stderr.shibboleth.idp.u2f.authn.api.DeviceDataStore
import eu.stderr.shibboleth.idp.u2f.authn.api.U2fPrincipal
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractValidationAction
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext
import net.shibboleth.idp.authn.principal.UsernamePrincipal
import net.shibboleth.idp.profile.ActionSupport
import net.shibboleth.utilities.java.support.primitive.StringSupport
import org.opensaml.profile.action.EventIds
import org.opensaml.profile.context.ProfileRequestContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import javax.annotation.Nonnull
import javax.security.auth.Subject
import javax.servlet.ServletRequest

@Slf4j
class ValidateResponse extends AbstractValidationAction {

    /** Spring injected data store */
    @Autowired
    @Qualifier('deviceDataStore')
    private DeviceDataStore dataStore

    private U2fUserContext u2fUserContext

    /** Constructor */
    /*public ValidateResponse() {
        super()
        log.debug("Constructor ValidateResponse()")
    }*/


    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                                   @Nonnull final AuthenticationContext authenticationContext) {
        final ServletRequest servletRequest = getHttpServletRequest()
        if (!servletRequest) {
            log.error("{} no ServletRequest is available", getLogPrefix())
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX)
            recordFailure()
            return false
        }

        u2fUserContext = authenticationContext.getSubcontext(U2fUserContext.class, true)
        if (!u2fUserContext) {
            log.error("{} no u2f user context exists", getLogPrefix())
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX)
            recordFailure()
            return false
        }

        def tokenResponse = StringSupport.trimOrNull(servletRequest.getParameter("tokenResponse"))
        if (!tokenResponse) {
            log.warn("{} no U2F response in the request", getLogPrefix())
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS)
            recordFailure()
            return false
        }
        u2fUserContext.tokenResponse = tokenResponse
        log.debug("{} got U2F response: {}", getLogPrefix(), tokenResponse)
        return true
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                             @Nonnull final AuthenticationContext authenticationContext) {
        //u2fUserContext = authenticationContext.getSubcontext(U2fUserContext.class, true)

        log.debug("{} validating U2F response for user {}", logPrefix, u2fUserContext.username)

        def json = new JsonSlurper()
        def tokenResponse = json.parseText(u2fUserContext.tokenResponse)
        /** Check for errorCode in response */
        if (tokenResponse?.errorCode) {
            log.debug("{} errorCode: ", logPrefix, tokenResponse)
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
            recordFailure()
        } else {
            def result = dataStore.finishAuthentication(u2fUserContext)
            if (result) {
                log.info("{} U2F login successful", logPrefix)
                recordSuccess()
                buildAuthenticationResult(profileRequestContext, authenticationContext)
            } else {
                handleError(profileRequestContext, authenticationContext, 'InvalidCredentials', AuthnEventIds.INVALID_CREDENTIALS)
                recordFailure()
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Subject populateSubject(@Nonnull final Subject subject) {
        log.debug("{} subjects {}", getLogPrefix(), subject.getPrincipals())
        subject.getPrincipals().add(new U2fPrincipal(u2fUserContext.username))
        return subject
    }

    /** {@inheritDoc} */
    @Override
    protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
                                             @Nonnull final AuthenticationContext authenticationContext) {
        super.buildAuthenticationResult(profileRequestContext, authenticationContext)
        log.debug("{} hmm username is: {}", getLogPrefix(), u2fUserContext.username)
        profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, true)
                .setPrincipalName((String) u2fUserContext.username)
    }
}
