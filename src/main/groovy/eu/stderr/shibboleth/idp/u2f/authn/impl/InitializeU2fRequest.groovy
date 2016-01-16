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

import com.sun.istack.internal.NotNull
import eu.stderr.shibboleth.idp.u2f.authn.DeviceDataStore
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractExtractionAction
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.authn.context.UsernamePasswordContext
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import org.opensaml.profile.context.ProfileRequestContext
import org.springframework.beans.factory.annotation.Value

import javax.annotation.Nonnull

@Slf4j
public class InitializeU2fRequest extends AbstractExtractionAction {

    /** Username context to get username */
    @Nonnull
    @NotEmpty
    private UsernamePasswordContext usernamePasswordContext

    @NotNull
    @NotEmpty
    private U2fUserContext u2fUserContext

    @Value('%{u2f.appId}')
    private final String appId

    /** Spring injected device data store */
    @Nonnull
    @NotEmpty
    private DeviceDataStore dataStore

    /** Inject device data store */
    void setDeviceDataStore(@Nonnull @NotEmpty final DeviceDataStore dataStore) {
        log.debug("Injecting data store")
        this.dataStore = dataStore
    }

    public InitializeU2fRequest() { super() }


    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                                @Nonnull final AuthenticationContext authenticationContext) {
        log.debug("${logPrefix} Entering doExecute")

        try {
            usernamePasswordContext = authenticationContext.getSubcontext(UsernamePasswordContext.class, true)
            u2fUserContext = authenticationContext.getSubcontext(U2fUserContext.class, true)
            if (!u2fUserContext.initialized) {
                u2fUserContext.username = usernamePasswordContext.username
                u2fUserContext.appId = appId
                dataStore.beginAuthentication(usernamePasswordContext.username, u2fUserContext)
                u2fUserContext.initialized = true
            }
        } catch (Exception e) {
            log.warn("${logPrefix} Error in doExecute", e)
        }
    }
}
