# SockRedirector

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/fce631c07eac48b682d8da9aee4b5301)](https://www.codacy.com/app/matteobaccan/SockRedirector?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matteobaccan/SockRedirector&amp;utm_campaign=Badge_Grade)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/matteobaccan/SockRedirector.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/matteobaccan/SockRedirector/context:java)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/matteobaccan/SockRedirector.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/matteobaccan/SockRedirector/alerts/)
[![Build Status](https://travis-ci.org/matteobaccan/SockRedirector.svg?branch=master)](https://travis-ci.org/matteobaccan/SockRedirector)
[![security status](https://www.meterian.io/badge/gh/matteobaccan/SockRedirector/security)](https://www.meterian.io/report/gh/matteobaccan/SockRedirector)
[![stability status](https://www.meterian.io/badge/gh/matteobaccan/SockRedirector/stability)](https://www.meterian.io/report/gh/matteobaccan/SockRedirector)
[![DepShield Badge](https://depshield.sonatype.org/badges/matteobaccan/SockRedirector/depshield.svg)](https://depshield.github.io)

<a href="https://github.com/matteobaccan/SockRedirector/stargazers"><img src="https://img.shields.io/github/stars/matteobaccan/SockRedirector" alt="Stars Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/network/members"><img src="https://img.shields.io/github/forks/matteobaccan/SockRedirector" alt="Forks Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/pulls"><img src="https://img.shields.io/github/issues-pr/matteobaccan/SockRedirector" alt="Pull Requests Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/issues"><img src="https://img.shields.io/github/issues/matteobaccan/SockRedirector" alt="Issues Badge"/></a>
<a href="https://github.com/matteobaccan/SockRedirector/graphs/contributors"><img alt="GitHub contributors" src="https://img.shields.io/github/contributors/matteobaccan/SockRedirector?color=2b9348"></a>
<a href="https://github.com/matteobaccan/SockRedirector/blob/master/LICENSE"><img src="https://img.shields.io/github/license/matteobaccan/SockRedirector?color=2b9348" alt="License Badge"/></a>
[![GraalVM Build](https://github.com/matteobaccan/SockRedirector/actions/workflows/graalvm.yml/badge.svg)](https://github.com/matteobaccan/SockRedirector/actions/workflows/graalvm.yml)

Redirects TCP connections from one IP address and port to another

I have used this tool for many years. This tool allow to redirect the TCP data from a port of one ip to another remote port of a remote machine.

This tool is very usefull in complex network architecture, where there are some firewall that are open only from one machine to another.

In this situation you can put sockRedirector server on thrusted machine, and connect to remote server using this machine

The concept is very similar to a proxy, without the limitation of use only http connection or the problem to write a socks interface.

Java sockRedirector is written in Java.

Use this program in Linux, Windows, AIX, AS/400 or all environment you want.

## Documentation

### sockRedirector.ini
Ini file is divided in several section For each section you can define these parameters

|key| type | default | value  |
|--|--|--|--|
| source | string | **mandatory** | source ip to bind, listen on |
| sourceport | int | **mandatory** | source port to bind, listen on |
| destination | string | **mandatory** | destionation to bind |
| destinationport | int | **mandatory** | destionation port to bind |
| log | boolean | true | Create a log under logs/sockRedirector.log |
| timeout | int | 0 | source socket timeout (seconds) |
| client | int | 10 | max connected client to the source |
| blocksize | int | 64000 | size of buffer to read from source and destination |
| inReadWait | long | 0 | reading from destination pause  |
| inWriteWait | long | 0 | writing to destination pause |
| outReadWait | long | 0 | reading from source pause |
| outWriteWait | long | 0 | write to source pause |

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
