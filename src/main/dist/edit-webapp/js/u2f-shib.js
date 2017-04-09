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


function u2fSign(data) {
    setTimeout(function() {
        u2f.sign(data.appId,
            data.challenge,
            data.registeredKeys,
            function(resp) {
                document.getElementById('tokenResponse').value = JSON.stringify(resp);
                document.getElementById('u2f_form').submit();
            });
    }, 1000);
}

function u2fRegiser(data) {
    u2f.register(data.appId,
        data.registerRequests,
        data.registeredKeys, function(resp) {
        if (resp.errorCode) {
            alert('Error: ' + resp.errorCode);
        } else {
            document.getElementById('u2f_data').value = JSON.stringify(resp);
            document.getElementById('u2f_form').submit();
        }
    });
}
