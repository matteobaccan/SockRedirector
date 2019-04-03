# SockRedirector
Redirects TCP connections from one IP address and port to another

---------------

I have used this tool for many years. This tool allow to redirect the
TCP data from a port of one ip to another remote port of a remote
machine.

This tool is very usefull in complex network architecture, where there
are some firewall that are open only from one machine to another.

In this situation you can put sockRedirector server on thrusted machine,
and connect to remote server using this machine

The concept is very similar to a proxy, without the limitation of use
only http connection or the problem to write a socks interface.

I have create a little sample inside the package, where
ftp.microsoft.com is hosted by localhost port 21. Get your favourite ftp
program, set passive mode, and try to connect to localhost.
sockRedirector get ftp.microsoft.com in realtime and display it in
transparent mode

I have also redirect www.libero.it. Try to tell to your browser to
navigare to localhost, and you'll see www.libero.it

Java sockRedirector is written in Java.

Use this program in Linux, Windows, AIX, AS/400 or all environment you
want.

sockRedirector.ini
------------------
Ini file is divided in several <redirection> </redirection> section
For each section you can define these parameters

<source> source ip to bind, for example: localhost

<sourceport> local port to bind.

<destination> destination host

<destinationport> destination port

Only for expert
---------------

<log> true/false (default), if you want to log data

<timeout> timeout for kill connection. 0 default = no timeout

<client> maximum number of client. 10 client is the default

<blocksize> maximum number of client. 64000 bytes is default size

<cache> true/false (default) cache data. All connection with the same
    request use the data cached from fist connection

<onlycache> true/false (default) use only data in cache. May be usefull
    after a navigation in cache mode, for simulate a remote host that is
    not available


Update:
-------

1.00
----
First public release

2.00
----
Some optimization and a new build
