MSHARED-154
==========
http://jira.codehaus.org/browse/MSHARED-154

Maven Archiver's `pomPropertiesFile` setting was never implemented. This
project attempts to fix this problem by reading the specified properties
file and applies values for `groupId`, `artifactId`, and `version` if
they are not present. Then, if the file needs to be written, it will
be stored at `target/maven-archiver/pom.properties` if possible,
otherwise the original file will be overwritten.
