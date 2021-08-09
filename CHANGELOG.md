# libhoney Changelog

## 1.3.2

Maintenance:

- Bump pmdCoreVersion from 6.36.0 to 6.37.0 (#61)
- Bump maven-compiler-plugin from 3.7.0 to 3.8.1 (#55)
- Bump maven-javadoc-plugin from 3.1.0 to 3.3.0 (#58)
- Bump wiremock from 2.17.0 to 2.27.2 (#60)
- Bump jackson-databind from 2.9.9.2 to 2.9.10.7 (#59)
- Bump nexus-staging-maven-plugin from 1.6.7 to 1.6.8 (#57)
- Bump jackson-core from 2.9.9 to 2.12.4 (#54)
- Bump pmdCoreVersion from 6.3.0 to 6.36.0 (#50)
- Bump maven-shade-plugin from 3.1.0 to 3.2.4 (#49)
- Bump maven-surefire-plugin from 2.22.1 to 2.22.2 (#48)

## 1.3.1

Fixes:

- Run the plugin to attach the thin jar with classifier after others to ensure it uses a valid file (#37).

## 1.3.0

Improvements:

- Add `HoneyClientBuilder.apiHost` overload that takes a prebuilt URI (#32).
- Provide a 'thin' version of libhoney with the `thin` classifer that does not include pinned dependencies (#35).

## 1.2.0

Improvements:

- Add default timeout for HTTP requests to 10 seconds. Previously there was no default which meant the client would would indefinetly.

Maintenance:
- Add CODEOWNERS file to automatically select PR reviewers.

## 1.1.2

Improvements:

- Provides a simplified Builder for HoneyClient, HoneyClientBuilder. See README in io.honeycomb.libhoney.builders package for usage.
- Added DefaultDebugResponseObserver to provide debug ResponseObserver for users who directly use libhoney.

## 1.1.1

Improvements:

- Adds raw message to response when unable to parse according to Honeycomb's error format. An example of this situation is when a proxy server is returning a response without hitting Honeycomb's server.
- Injects version to manifest via Maven and modifies user agent to use injected version rather than hardcoded value. This solves [#17](https://github.com/honeycombio/libhoney-java/issues/17).

## 1.1.0

Improvements:

- Expands the suite of options available to configure libhoney's HTTP behavior and allows setting a CredentialProvider (enabling basic auth, etc.) as well as a SSLContext (for custom certificate / SSL activity).

## 1.0.9

Improvements:

- Adds a proxy `TransportOption`

## 1.0.8

Security Updates:

- Upgrade Jackson to 2.9.9.2 for security patches which address the following:
  [CVE-2019-14439](https://nvd.nist.gov/vuln/detail/CVE-2019-14439)
  [CVE-2019-14379](https://nvd.nist.gov/vuln/detail/CVE-2019-14379)
  [CVE-2019-12814](https://nvd.nist.gov/vuln/detail/CVE-2019-12814)
  [CVE-2019-12384](https://nvd.nist.gov/vuln/detail/CVE-2019-12384)
  [CVE-2019-12086](https://nvd.nist.gov/vuln/detail/CVE-2019-12086)

## 1.0.7 2019-06-06

Fixes:

- Adds slf4j as a normal dependency to avoid version conflicts.

## 1.0.6 2019-02-06

Improvements:

- Only log 401 Unauthorized errors if there are no ResponseObservers attached to handle them. Previously, libhoney-java would continue logging even if a ResponseObserver was attached, leading to a lot of noise in the logs.

## 1.0.5 2019-02-04 Update recommended

Fixes:

- Timestamp formatting caused an extra zero to be added in front of the millisecond portion, resulting in events appearing with an incorrect timestamp in our UI.

## 1.0.4 2019-02-04 Update recommended

Security Updates:

- Upgrade Jackson to 2.9.8 for security patches which address the following: [CVE-2018-14718](https://nvd.nist.gov/vuln/detail/CVE-2018-14718), [CVE-2018-14719](https://nvd.nist.gov/vuln/detail/CVE-2018-14719), [CVE-2018-14720](https://nvd.nist.gov/vuln/detail/CVE-2018-14720), [CVE-2018-14721](https://nvd.nist.gov/vuln/detail/CVE-2018-14721), [CVE-2018-19360](https://nvd.nist.gov/vuln/detail/CVE-2018-19360), [CVE-2018-19361](https://nvd.nist.gov/vuln/detail/CVE-2018-19361), [CVE-2018-19362](https://nvd.nist.gov/vuln/detail/CVE-2018-19362), [CVE-2018-1000873](https://nvd.nist.gov/vuln/detail/CVE-2018-1000873).

## 1.0.3 2019-01-30

Improvements:

- Adds a configuration option for the additional user agent string required by the Honeycomb Beeline for Java.
