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
import groovy.transform.AutoClone
import groovy.transform.EqualsAndHashCode
import net.shibboleth.idp.authn.principal.CloneablePrincipal
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import net.shibboleth.utilities.java.support.logic.Constraint
import net.shibboleth.utilities.java.support.primitive.StringSupport

import javax.annotation.Nonnull

@AutoClone
@EqualsAndHashCode
class U2fPrincipal implements CloneablePrincipal {
    @Nonnull
    @NotEmpty
    private final def username

    public U2fPrincipal(@Nonnull @NotEmpty final String username) {
        this.username = Constraint.isNotNull(StringSupport.trimOrNull(username), 'Username may not be null or empty')
    }

    @Override
    String getName() {
        return this.username
    }
}
