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

package eu.stderr.shibboleth.idp.u2f.c14n
import com.google.common.base.Predicate
import eu.stderr.shibboleth.idp.u2f.authn.impl.U2fPrincipal
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext
import org.opensaml.profile.context.ProfileRequestContext

import javax.annotation.Nullable

class U2fSubjectCanonicalization implements Predicate<ProfileRequestContext> {
    @Override
    boolean apply(@Nullable ProfileRequestContext profileRequestContext) {
        if (profileRequestContext) {
            final def u2fPrincipals = profileRequestContext.getSubcontext(SubjectCanonicalizationContext, false)?.getSubject()?.getPrincipals(U2fPrincipal)
            return u2fPrincipals && u2fPrincipals.size() == 1
        }
        return false
    }
}
