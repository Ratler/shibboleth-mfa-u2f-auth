/*
 *  Copyright 2017 Stefan Wold <ratler@stderr.eu>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.stderr.shibboleth.idp.u2f.authn.impl

import eu.stderr.shibboleth.idp.u2f.authn.api.DeviceDataStore
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.profile.AbstractProfileAction
import net.shibboleth.idp.profile.ActionSupport
import net.shibboleth.utilities.java.support.primitive.StringSupport
import org.opensaml.profile.context.ProfileRequestContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import javax.annotation.Nonnull
import javax.servlet.http.HttpServletRequest

@Slf4j
class FinishRegisterNewDevice extends AbstractProfileAction {
    @Autowired
    @Qualifier('deviceDataStore')
    DeviceDataStore dataStore

    /** User context */
    U2fUserContext userContext


    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(ProfileRequestContext profileRequestContext) {
        try {
            userContext = profileRequestContext.getSubcontext(AuthenticationContext.class)
                    .getSubcontext(U2fUserContext.class, true)
            return true
        } catch (Exception e) {
            log.error("Error in doPreExecute", e)
            return false
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        log.debug("{} Entering doExecute", getLogPrefix())
        final HttpServletRequest request = getHttpServletRequest()

        if (!request) {
            log.error("{} HttpServletRequest is empty", getLogPrefix())
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.ACCOUNT_ERROR)
            return
        }

        def registrationDataResponse = StringSupport.trimOrNull(request.getParameter("u2f_data"))
        userContext.registrationDataResponse = registrationDataResponse
        log.debug("{} U2F device registration data for user {}: {}", getLogPrefix(), userContext.username, registrationDataResponse)
        try {
            dataStore.finishRegistration(userContext)
        } catch (Exception e) {
            log.debug("{} U2F device registration failed for user {]: {]", getLogPrefix(), userContext.username, e)
        }
    }
}
