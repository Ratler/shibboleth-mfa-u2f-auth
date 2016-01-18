> This project is work in progress.

# Shibboleth U2F Authentication Plugin
U2F multi-factor authentication plugin for Shibboleth IdPv3.

## Notes
Tested with Shibboleth Identity Provider 3.2.x and Google Chrome 47.x. Currently supported U2F device data stores are DummyDataStore (for
testing only), and [Yubico's U2F Validation server](https://developers.yubico.com/u2fval/).

Generic support for SQL and MongoDB will be available shortly. I18N support is on the ToDo. Make initial configuration easier by leveraging the power of the `Spring framework`.

## Requirements
* [Shibboleth Identity Provider 3.2.x](http://shibboleth.net/downloads/identity-provider/latest/)
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

2. Copy `conf`, `edit-webapp`, `flows` and `views` to $IDP_HOME, usually /opt/shibboleth-idp.
  ```
  $ cp -r build/install/shibboleth-mfa-u2f-auth/* $IDP_HOME/
  ```

3. Edit `$IDP_HOME/conf/u2f.properties` and change the property `u2f.appId` to your preferred application ID, see [https://developers.yubico.com/U2F/App_ID.html](https://developers.yubico.com/U2F/App_ID.html) for help.

4. Edit `$IDP_HOME/conf/idp.properties` and change the following properties:
  * Append `/conf/u2f.properties` to the property `idp.additionalProperties=`, eg `idp.additionalProperties= /conf/ldap.properties, /conf/saml-nameid.properties, /conf/services.properties, /conf/u2f.properties`
  * Append `u2f` to the property `idp.authn.flows= `, eg `idp.authn.flows= Password|u2f`

5. (Optional) Edit `$IDP_HOME/conf/authn/u2f-authn-beans.xml` to change your preferred U2F device data store, default is [Yubico's U2F Validation server](https://developers.yubico.com/u2fval/).

6. Edit `$IDP_HOME/conf/authn/u2f-authn-config.xml` to add U2F device data store configuration, eg U2fval API endpoint and authentication credentials.

7. Add `authn/u2f` bean to `$IDP_HOME/conf/authn/general-authn.xml`, to the element `<util:list id="shibboleth.AvailableAuthenticationFlows">`
  ```
  <bean id="authn/u2f" parent="shibboleth.AuthenticationFlow"
      p:passiveAuthenticationSupported="true"
      p:forcedAuthenticationSupported="true">
      <property name="supportedPrincipals">
          <util:list>
              <bean parent="shibboleth.SAML2AuthnContextClassRef"
                  c:classRef="http://stderr.eu/u2f" />
              <bean parent="shibboleth.SAML2AuthnContextClassRef"
                  c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport" />
              <bean parent="shibboleth.SAML1AuthenticationMethod"
                  c:method="http://stderr.eu/u2f" />
          </util:list>
      </property>
  </bean>        
  ```

8. Add `c14n/u2f` bean to `$IDP_HOME/conf/c14n/subject-c14n.xml`, to the element `<util:list id="shibboleth.PostLoginSubjectCanonicalizationFlows">`
  ```
  <bean id="c14n/u2f" parent="shibboleth.PostLoginSubjectCanonicalizationFlow">
      <property name="activationCondition">
          <bean class="eu.stderr.shibboleth.idp.u2f.c14n.U2fSubjectCanonicalization"/>
      </property>
  </bean>
  ```
9. Rebuild the IdP war file
  ```
  $ $IDP_HOME/bin/build.sh
  ```

### (Optional) Alternative flow configuration
If the property `idp.authn.flows.initial= ` is required in `$IDP_HOME/conf/idp.properties`, eg `idp.authn.flows.initial= Password`, the flow configuration needs to be tweaked a little bit. In addition to the configuration from step 3 to 8 do the following, then rebuild the WAR as in step 9:
* Edit `$IDP_HOME/flows/authn/conditions/conditions-flow.xml` and add the following just below the tag ` <action-state id="ValidateUsernamePassword">`
  ```
  <!-- Enable u2f -->
  <evaluate expression="ValidateUsernamePassword" />
  <evaluate expression="'u2f'" />
  <transition on="u2f" to="U2fAuth" />
  <!-- End u2f -->
  ```
  also in the same file just before the tag `</flow>` add the following:
  ```
  <subflow-state id="U2fAuth" subflow="authn/u2f">
      <input name="calledAsSubflow" value="true" />
      <transition on="proceed" to="proceed" />
  </subflow-state>
  ```
*  Edit `$IDP_HOME/flows/authn/u2f/u2f-flow.xml` and disable the subflow tag, eg:
  ```
  <!--
  <subflow-state id="CallPasswordFlow" subflow="authn/Password">
      <input name="calledAsSubflow" value="true" />
      <transition on="proceed" to="displayU2fForm" />
  </subflow-state>
  -->
  ```
