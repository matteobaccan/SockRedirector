# SockRedirector

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/fce631c07eac48b682d8da9aee4b5301)](https://www.codacy.com/app/matteobaccan/SockRedirector?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matteobaccan/SockRedirector&amp;utm_campaign=Badge_Grade)
[![security status](https://www.meterian.io/badge/gh/matteobaccan/SockRedirector/security)](https://www.meterian.io/report/gh/matteobaccan/SockRedirector)
[![stability status](https://www.meterian.io/badge/gh/matteobaccan/SockRedirector/stability)](https://www.meterian.io/report/gh/matteobaccan/SockRedirector)

<a href="https://github.com/matteobaccan/SockRedirector/stargazers"><img src="https://img.shields.io/github/stars/matteobaccan/SockRedirector" alt="Stars Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/network/members"><img src="https://img.shields.io/github/forks/matteobaccan/SockRedirector" alt="Forks Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/pulls"><img src="https://img.shields.io/github/issues-pr/matteobaccan/SockRedirector" alt="Pull Requests Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/issues"><img src="https://img.shields.io/github/issues/matteobaccan/SockRedirector" alt="Issues Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/graphs/contributors"><img alt="GitHub contributors" src="https://img.shields.io/github/contributors/matteobaccan/SockRedirector?color=2b9348"></a>
<a href="https://github.com/matteobaccan/SockRedirector/blob/master/LICENSE"><img src="https://img.shields.io/github/license/matteobaccan/SockRedirector?color=2b9348" alt="License Badge"/></a>
[![GraalVM Build](https://github.com/matteobaccan/SockRedirector/actions/workflows/graalvm.yml/badge.svg)](https://github.com/matteobaccan/SockRedirector/actions/workflows/graalvm.yml)

Redirects TCP connections from one IP address and port to another.

I have used this tool for many years. It redirects the TCP traffic received on a local address/port to a port of a remote machine.

It is very useful in complex network architectures, where firewalls allow connections only from one specific machine to another:
you can run SockRedirector on the trusted machine and reach the remote server through it.

The concept is very similar to a proxy, without being limited to HTTP connections and without the need to implement a SOCKS interface.

SockRedirector is written in Java and runs on Linux, Windows, macOS, AIX, AS/400 or any environment with a Java runtime.
Native executables (built with GraalVM) are also available for Linux, macOS and Windows.

## Requirements

- Java 11 or later to run the jar
- To build: JDK 25 and Maven (GraalVM 25 to build the native executable)

## Build

```bash
# Runnable jar: target/SockRedirector-<version>-jar-with-dependencies.jar
mvn package

# Native executable (requires GraalVM): target/Sockredirector
mvn package -DskipNativeVersion=false
```

## Run

Put a `sockRedirector.ini` file in the working directory and start the program:

```bash
java -jar SockRedirector-2.0.5-jar-with-dependencies.jar
```

Logs are written to the console and to `logs/sockRedirector.log`.

## Documentation

### sockRedirector.ini
The ini file is divided into one or more `<redirection>` sections. For each section you can define these parameters

|key| type | default | value  |
|--|--|--|--|
| source | string | **mandatory** | source ip to bind, listen on |
| sourceport | int | **mandatory** | source port to bind, listen on |
| destination | string | **mandatory** | destination host |
| destinationport | int | **mandatory** | destination port |
| log | boolean | false | dump the traffic of each connection under `logs/` |
| timeout | int | 0 | source socket timeout (seconds), 0 = no timeout |
| client | int | 10 | max pending connections on the source (backlog) |
| blocksize | int | 64000 | size of buffer to read from source and destination |
| inReadWait | long | 0 | pause before reading from destination (ms) |
| inWriteWait | long | 0 | pause before writing to source (ms) |
| outReadWait | long | 0 | pause before reading from source (ms) |
| outWriteWait | long | 0 | pause before writing to destination (ms) |
| randomKill | long | 0 | testing only: randomly close connections (about once every N seconds), 0 = disabled |

## Example
### Configuration Example (sockRedirector.ini)

```xml
<redirection>
   <source>127.0.0.1</source>
   <sourceport>80</sourceport>
   <destination>1.1.1.1</destination>
   <destinationport>80</destinationport>
   <log>true</log>
   <timeout>0</timeout>
   <client>50</client>
   <inReadWait>0</inReadWait>
   <inWriteWait>0</inWriteWait>
   <outReadWait>0</outReadWait>
   <outWriteWait>1000</outWriteWait>
</redirection>
```

## Admin console

While running, SockRedirector accepts these commands on standard input:

| command | description |
|--|--|
| help | show the command list |
| exit | exit program |
| thread [filter] | list active connections |
| kill &lt;id&gt; [&lt;id&gt; ...] | close the given threads |
| pause &lt;id&gt; &lt;readPause&gt; &lt;writePause&gt; | set read/write pause (ms) on a flow thread |

## QUESTIONS AND DOCUMENTATION

For interact with the **SockRedirector Documentation**, visit [Deep Wiki](https://deepwiki.com/matteobaccan/SockRedirector).
