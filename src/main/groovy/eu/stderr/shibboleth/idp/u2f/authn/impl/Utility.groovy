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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Slf4j
class Utility {
    @Autowired
    @Qualifier('deviceDataStore')
    DeviceDataStore dataStore

    public haveU2f(U2fUserContext userContext) {
        return dataStore.hasU2fDevice(userContext.username)
    }
}
