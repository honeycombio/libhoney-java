# libhoney Changelog

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
