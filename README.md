MSHARED-154
==========
http://jira.codehaus.org/browse/MSHARED-154

Maven Archiver's `pomPropertiesFile` setting was never implemented. This
project attempts to fix this problem by reading the specified properties
file, applying values for `groupId`, `artifactId`, and `version`, and
then rewriting the properties file in-place.

