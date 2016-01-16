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
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractExtractionAction
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.profile.ActionSupport
import net.shibboleth.utilities.java.support.primitive.StringSupport
import org.opensaml.profile.context.ProfileRequestContext

import javax.annotation.Nonnull
import javax.servlet.http.HttpServletRequest

@Slf4j
class ProcessU2fResponse extends AbstractExtractionAction {

    /** Constructor */
    public ProcessU2fResponse() {
        super()
    }

    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                             @Nonnull final AuthenticationContext authenticationContext) {
        final HttpServletRequest request = getHttpServletRequest()

        if (!request) {
            log.debug("{} HttpServletRequest is empty", getLogPrefix())
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS)
            return
        }

        U2fUserContext u2fUserContext = authenticationContext.getSubcontext(U2fUserContext.class, true)

        def tokenResponse = StringSupport.trimOrNull(request.getParameter("tokenResponse"))
        log.debug("{} U2F tokenResponse for user {}: {}", getLogPrefix(), u2fUserContext.username, tokenResponse)

        u2fUserContext.tokenResponse = tokenResponse
    }
}
