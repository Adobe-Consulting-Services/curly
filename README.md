# Curly
Curly is a multi-threaded task runner for AEM maintenance operations.  It is meant to supplement and/or replace the traditional use of cURL commands.

## Requirements
Java 11 is required to use this utility.  Maven (v3 or higher) is required to build this from source code.  All other dependencies are retrieved automatically by the Maven build process.  Note that a build is platform-specific because it includes platform-specific JavaFX libraries.

## Documentation
More information about Curly is available in the wiki:

+ [Getting Started / Installation](https://github.com/badvision/curly/wiki/How-to-Start-Curly)
+ [Basic Usage](https://github.com/badvision/curly/wiki/How-to-Use-Curly)
+ [Batch Processing](https://github.com/badvision/curly/wiki/Batch-Processing)
+ [Reporting](https://github.com/badvision/curly/wiki/Reporting)

## Getting Curly
Right now I don't have a release process in place, and with Java 11 that complicates things now that I have to include platform-specific JFX libraries.  Once I get this worked out I'll update this page with download links.  In the meantime you can grab the source and build with maven (mvn install) and the full release jar will be in your target folder when it completes.  Note that if you build on Windows, you will have a windows-only build, and so on.  This is, again, because I have to include JFX native libraries since Oracle in their infinite wisdom (ha!) decided it was a great idea to remove JavaFX from the standard Java Runtime builds from Java 9 onward.
