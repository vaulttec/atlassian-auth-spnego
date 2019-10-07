# Seraph Authenticators for Auto-Login support in Atlassian Confluence / JIRA

This project provides [Seraph](https://docs.atlassian.com/atlassian-seraph/) Authenticators for Atlassian's Confluence and JIRA which implement auto-login via [GSSAPI](http://en.wikipedia.org/wiki/GSSAPI) / [SPNEGO](http://en.wikipedia.org/wiki/SPNEGO) (e.g. [Kerberos](http://en.wikipedia.org/wiki/Kerberos_%28protocol%29#Microsoft_Windows) / Active Directory).  

The GSSAPI / SPNEGO support is provided by the [SPNEGO SourceForge project](http://spnego.sourceforge.net/).  

## Installation

Download the product-specific authenticator JAR file from the [releases tab](https://github.com/vaulttec/atlassian-auth-spnego/releases) and copy it to the Atlassian product's web app library folder `/WEB-INF/lib/`.


## Configuration

To use the SPNEGO authenticators the following configuration is needed.


### SPNEGO Configuration

The [SPNEGO framework configuration](http://spnego.sourceforge.net/reference_docs.html) for a single backend (e.g. Kerberos server) is provided in a Java properties file (which is used later on in `seraph-config.xml`), e.g.

```
name=Kerberos Server 1
spnego.allow.basic=false
spnego.allow.unsecure.basic=false
spnego.allow.localhost=false
spnego.login.client.module=spnego-client
spnego.krb5.conf=/var/atlassian/spnego/krb5.conf
spnego.login.conf=/var/atlassian/spnego/login.conf
spnego.login.server.module=spnego-server
spnego.prompt.ntlm=false
spnego.allow.delegation=true
spnego.logger.level=6
spnego.preauth.username={Kerberos User Account}
spnego.preauth.password={Kerberos User Password}
```

In this properties file are additional configuration files (`krb5.conf` and `login.conf`) referenced. Take a look at the SPNEGO framework's [pre-flight](http://spnego.sourceforge.net/pre_flight.html) documentation for guidance on how to create these files. 


### Seraph Configuration

In the Atlassian product's [seraph configuration `/WEB-INF/classes/seraph-config.xml`](https://docs.atlassian.com/atlassian-seraph/latest/configuration.html) the existing Seraph Authenticator has to be replaced by our own product-specific authenticator:

```xml
<authenticator class="org.vaulttec.atlassian.auth.{Confluence|Jira}SpnegoAuthenticator">
  <init-param>
    <!-- comma-separated list of SPNEGO config files (absolute path) -->
    <param-name>config.files</param-name>
    <param-value>/var/atlassian/spnego/kerberos-server1.properties, /var/atlassian/spnego/kerberos-server2.properties</param-value>
  </init-param>
  <init-param>
    <!-- comma-separated list of included URIs -->
    <param-name>include.uris</param-name>
    <param-value>/login.jsp?*os_destination=*</param-value>
  </init-param>
  <init-param>
    <!-- comma-separated list of excluded URIs -->
    <param-name>exclude.uris</param-name>
    <param-value>/rest/*, /plugins/*</param-value>
  </init-param>
</authenticator>
```

#### URI Syntax

The URIs provided (as comma-separated list) for the parameter `include.uris` and `exclude.uris` consist of two parts - a leading path and an optional trailing query string (separated by "?"), e.g. `/logout` or `/login.jsp?os_destination=/admin/`. These URIs are checked for **exact matching** (same case and length).

Both parts support an optional leading and / or trailing wildcard (indicated by "*"), e.g. `/startswith/*`, `*/endswith`, `*/substring/*`, `/withquery?query1=*` or `/withquery?*query2=true*`. Due to performance reasons only a single **trailing** and / or **leading** wildcard is allowed.


## Confluence Configuration

For Confluence the Seraph configuration (located in `<CONFLUENCE_INST_PATH>/confluence/WEB-INF/classes/seraph-config.xml`) has to be changed as follows:

```xml
<!-- <authenticator class="com.atlassian.confluence.user.ConfluenceAuthenticator"/> -->

<authenticator class="org.vaulttec.atlassian.auth.ConfluenceSpnegoAuthenticator">
  <init-param>
    <!-- comma-separated list of SPNEGO config files (absolute path) -->
    <param-name>config.files</param-name>
    <param-value>/var/confluence/spnego/kerberos-server1.properties, /var/confluence/spnego/kerberos-server2.properties</param-value>
  </init-param>
  <init-param>
    <!-- comma-separated list of excluded URIs -->
    <param-name>exclude.uris</param-name>
    <param-value>/rest/*, /plugins/*, /images/*, /download/*, /styles/*, /s/*, /login.action, /logout.action</param-value>
  </init-param>
</authenticator>
```

## JIRA Configuration

For JIRA the Seraph configuration (located in `<JIRA_INST_PATH>/atlassian-jira/WEB-INF/classes/seraph-config.xml`) has to be changed as follows:

```xml
<!-- authenticator class="com.atlassian.jira.security.login.JiraSeraphAuthenticator"/-->

<authenticator class="org.vaulttec.atlassian.auth.JiraSpnegoAuthenticator">
  <init-param>
    <!-- comma-separated list of SPNEGO config files (absolute path) -->
    <param-name>config.files</param-name>
    <param-value>/var/jira/spnego/kerberos-server1.properties, /var/jira/spnego/kerberos-server2.properties</param-value>
  </init-param>
  <init-param>
    <!-- comma-separated list of included URIs -->
    <param-name>include.uris</param-name>
    <param-value>/login.jsp?*os_destination=*</param-value>
  </init-param>
  <init-param>
    <!-- comma-separated list of excluded URIs -->
    <param-name>exclude.uris</param-name>
    <param-value>/rest/*, /plugins/*, /images/*, /download/*, /s/*, /login.jsp, /logout, /secure/Logout*, /alreadyloggedout.jsp</param-value>
  </init-param>
</authenticator>
```

## Troubleshooting

The authenticator's logging can be added to the Atlassian product's application log. Therefore create a new logging entry for the package `org.vaulttec.atlassian.auth` in the product's "Logging and Profiling" admin frontend.
