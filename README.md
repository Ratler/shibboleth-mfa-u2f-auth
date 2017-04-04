> This project is work in progress.

# Shibboleth U2F Authentication Plugin
U2F authentication flow for Shibboleth Identity Provider v3.3.x. The U2F flow is designed to be
used together with another login flow, usually by utilizing the MFA login flow.

## Notes
Tested with Shibboleth Identity Provider 3.3.x, Google Chrome 57.x and Opera 43.x. Currently supported U2F device data
stores are DummyDataStore (for testing only), and [Yubico's U2F Validation server](https://developers.yubico.com/u2fval/).

## Requirements
* [Shibboleth Identity Provider 3.3.x](http://shibboleth.net/downloads/identity-provider/latest/)
* Java 8

## Installation

1. Download preferred distribution, binary release or building from source

    * Binary release

    Download from [https://github.com/Ratler/shibboleth-mfa-u2f-auth/releases](https://github.com/Ratler/shibboleth-mfa-u2f-auth/releases)

    * Source

      Building the distribution.
      ```
      $ git clone https://github.com/Ratler/shibboleth-mfa-u2f-auth.git
      $ cd shibboleth-mfa-u2f-auth
      $ ./gradlew clean installDist
      ```
      Files will be found in build/install/shibboleth-mfa-u2f-auth.

2. Copy `conf`, `edit-webapp` and `views` to $IDP_HOME, usually /opt/shibboleth-idp.
  ```
  $ cp -r build/install/shibboleth-mfa-u2f-auth/* $IDP_HOME/
  ```

3. Copy `$IDP_HOME/conf/u2f.properties.dist` to `$IDP_HOME/conf/u2f.properties` then edit `$IDP_HOME/conf/u2f.properties`
and change the property `u2f.appId` to your preferred application ID,
see [https://developers.yubico.com/U2F/App_ID.html](https://developers.yubico.com/U2F/App_ID.html) for help. Enable and
configure one of the supported data stores, u2fval is recommended.


4. Edit `$IDP_HOME/conf/idp.properties` and change the following properties:
  * Append `/conf/u2f.properties` to the property `idp.additionalProperties=`, eg `idp.additionalProperties= /conf/ldap.properties, /conf/saml-nameid.properties, /conf/services.properties, /conf/u2f.properties`
  * Change the property `idp.authn.flows=` to `idp.authn.flows=MFA`

5. Edit `$IDP_HOME/conf/authn/general-authn.xml`, add `authn/U2f` bean to the element `<util:list id="shibboleth.AvailableAuthenticationFlows">`
```
    <bean id="authn/U2f" parent="shibboleth.AuthenticationFlow"
        p:passiveAuthenticationSupported="true"
        p:forcedAuthenticationSupported="true">
        <property name="supportedPrincipals">
            <util:list>
                <bean parent="shibboleth.SAML2AuthnContextClassRef"
                    c:classRef="http://stderr.eu/u2f" />
                <bean parent="shibboleth.SAML1AuthenticationMethod"
                    c:method="http://stderr.eu/u2f" />
            </util:list>
        </property>
    </bean>
```

Modify the supportedPrincipals list in the bean `<bean id="authn/MFA"...` to something like this:

```
    <property name="supportedPrincipals">
        <list>
            <bean parent="shibboleth.SAML2AuthnContextClassRef"
                c:classRef="http://stderr.eu/u2f" />
            <bean parent="shibboleth.SAML1AuthenticationMethod"
                c:method="http://stderr.eu/u2f" />
            <bean parent="shibboleth.SAML2AuthnContextClassRef"
                c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport" />
            <bean parent="shibboleth.SAML2AuthnContextClassRef"
                c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:Password" />
            <bean parent="shibboleth.SAML1AuthenticationMethod"
                c:method="urn:oasis:names:tc:SAML:1.0:am:password" />
        </list>
    </property>
```

6. Edit `$IDP_HOME/conf/authn/mfa-authn-config.xml` and change the element `<util:map id="shibboleth.authn.MFA.TransitionMap">`
to something like this:

```
    <util:map id="shibboleth.authn.MFA.TransitionMap">
        <!-- First rule runs the UsernamePassword login flow. -->
        <entry key="">
            <bean parent="shibboleth.authn.MFA.Transition" p:nextFlow="authn/Password" />
        </entry>

        <!-- An implicit final rule will return whatever the final flow returns. -->
        <entry key="authn/Password">
            <bean parent="shibboleth.authn.MFA.Transition" p:nextFlow="authn/U2f" />
        </entry>
    </util:map>
```

The MFA flow above is the simplest form. The MFA login flow provides a scriptable (or programmable) way to combine one
or more login flows, see
[https://wiki.shibboleth.net/confluence/display/IDP30/MultiFactorAuthnConfiguration](https://wiki.shibboleth.net/confluence/display/IDP30/MultiFactorAuthnConfiguration)
for more information.

7. Rebuild the IdP war file
  ```
  $ $IDP_HOME/bin/build.sh
  ```
