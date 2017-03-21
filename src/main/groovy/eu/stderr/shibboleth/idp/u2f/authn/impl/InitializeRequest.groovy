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

import com.google.common.base.Function
import com.sun.istack.internal.NotNull
import eu.stderr.shibboleth.idp.u2f.authn.api.DeviceDataStore
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractExtractionAction
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.authn.context.UsernamePasswordContext
import net.shibboleth.idp.profile.ActionSupport
import net.shibboleth.idp.session.context.navigate.CanonicalUsernameLookupStrategy
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import org.opensaml.profile.context.ProfileRequestContext
import org.opensaml.soap.wssecurity.Username
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.webflow.execution.Event

import javax.annotation.Nonnull
import javax.annotation.Nullable

@Slf4j
public class InitializeRequest extends AbstractExtractionAction {

    @NotNull
    @NotEmpty
    private U2fUserContext u2fUserContext

    /** Attempted username. */
    @Nullable
    @NotEmpty
    private String username

    /** U2F application ID */
    @Value('%{u2f.appId}')
    private final String appId

    /** Spring injected device data store */
    @Autowired
    @Qualifier('deviceDataStore')
    private DeviceDataStore dataStore

    /** Lookup strategy for username to match against u2f identity. */
    @Autowired
    @Qualifier('CanonicalUsernameStrategy')
    @Nonnull
    private Function<ProfileRequestContext,String> usernameLookupStrategy

    public InitializeRequest() {
        super()
        usernameLookupStrategy = new CanonicalUsernameLookupStrategy()
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                                   @Nonnull final AuthenticationContext authenticationContext) {
        u2fUserContext = authenticationContext.getSubcontext(U2fUserContext.class, true)
        username = usernameLookupStrategy.apply(profileRequestContext)
        if (!username) {
            log.warn("{} No principal name available to cross-check U2F result", getLogPrefix())
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS)
            return false
        }

        return true
    }

    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                             @Nonnull final AuthenticationContext authenticationContext) {
        log.debug("${logPrefix} Entering doExecute")

        try {

            log.debug("{} Principal name {}", getLogPrefix(), username)
            if (!u2fUserContext.initialized) {
                u2fUserContext.username = username
                u2fUserContext.appId = appId
                u2fUserContext.initialized = true
            }
            def res = dataStore.beginAuthentication(u2fUserContext)
            if (!res) {
                def state = u2fUserContext.state
                log.debug("beginAuthentication() failed with state: {}", state)
                // Reset state
                u2fUserContext.state = ""
                ActionSupport.buildEvent(profileRequestContext, state)
            }
        } catch (Exception e) {
            log.warn("{} Error in doExecute", getLogPrefix(), e)
        }
    }

    public Event haveU2f(ProfileRequestContext profileRequestContext) {
        username = usernameLookupStrategy.apply(profileRequestContext)

        if (username && dataStore.hasU2fDevice(username)) {
            log.debug("{} U2F device found for user {}", getLogPrefix(), username)
            return new Event(this, 'HaveU2f')
        }
        log.debug("{} No U2F device found for user {}", getLogPrefix(), username)
        return new Event(this, 'NoU2f')
    }
}
