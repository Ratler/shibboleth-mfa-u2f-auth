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
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.profile.AbstractProfileAction
import org.opensaml.profile.context.ProfileRequestContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.webflow.execution.Event
import org.springframework.webflow.execution.RequestContext

import javax.annotation.Nonnull

@Slf4j
class BeginRegisterNewDevice extends AbstractProfileAction {
    /** Spring injected data store */
    @Autowired
    @Qualifier('deviceDataStore')
    private DeviceDataStore dataStore

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
            log.debug("Error in doPreExecute", e)
            return false
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Event doExecute(@Nonnull final RequestContext requestContext,
                              @Nonnull final ProfileRequestContext profileRequestContext) {
        def authenticationContext = profileRequestContext.getSubcontext(AuthenticationContext.class)
        U2fUserContext userContext = authenticationContext.getSubcontext(U2fUserContext.class, true)
        try {
            if (dataStore.hasU2fDevice(userContext.username)) {
                return new Event(this, 'HaveU2f')
                //return ActionSupport.buildEvent(profileRequestContext, 'HaveU2f')
            } else {
                dataStore.beginRegistration(userContext.username)
                return new Event(this, 'proceed')
            }
            //return ActionSupport.buildEvent(profileRequestContext, 'proceed')
        } catch (Exception e) {
            log.debug("Failed to initialize device registration", e)
        }
    }
}
