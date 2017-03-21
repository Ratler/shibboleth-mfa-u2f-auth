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

package eu.stderr.shibboleth.idp.u2f.authn.api

import com.google.common.base.MoreObjects
import groovy.transform.EqualsAndHashCode
import net.shibboleth.idp.authn.principal.CloneablePrincipal
import net.shibboleth.utilities.java.support.annotation.ParameterName
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import net.shibboleth.utilities.java.support.logic.Constraint
import net.shibboleth.utilities.java.support.primitive.StringSupport

import javax.annotation.Nonnull


//@AutoClone
@EqualsAndHashCode
class U2fPrincipal implements CloneablePrincipal {
    @Nonnull
    @NotEmpty
    private String username

    /**
     * Constructor
     *
     * @param username
     */
    U2fPrincipal(@Nonnull @NotEmpty @ParameterName(name="name") final String name) {
        username = Constraint.isNotNull(StringSupport.trimOrNull(name), 'Username may not be null or empty')
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty String getName() {
        return username
    }

    /** {@inheritDoc} */
    @Override
    String toString() {
        return MoreObjects.toStringHelper(this).add("username", username).toString()
    }


    /** {@inheritDoc} */
    @Override
    U2fPrincipal clone() throws CloneNotSupportedException {
        final U2fPrincipal copy = (U2fPrincipal) super.clone()
        copy.username = username
        return copy
    }
}
