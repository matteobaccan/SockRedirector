/*
 * portRedirector Engine
 *
 * Copyright 2003 Matteo Baccan <baccan@infomedia.it>
 * www - http://www.infomedia.it/artic/Baccan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA (or visit
 * their web site at http://www.gnu.org/).
 *
 */
package it.baccan.sockredirector;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Matteo Baccan <matteo@baccan.it>
 */
@Slf4j
public class PortRedirect extends Thread {

    /**
     *
     */
    private static final AtomicLong THREADTOTAL = new AtomicLong(0);
    private String sourceAddress;
    private int sourcePort;
    private String destinationAddress;
    private int destinationPort;
    private boolean logger;
    private boolean cache;
    private boolean onlycache;
    private int timeout;
    private int maxclient;
    private int blockSize;

    /**
     *
     * @param cXML
     */
    public PortRedirect(String cXML) {

        this.sourceAddress = getToken(cXML, "source");
        this.sourcePort = Integer.parseInt(getToken(cXML, "sourceport"));
        this.destinationAddress = getToken(cXML, "destination");
        this.destinationPort = Integer.parseInt(getToken(cXML, "destinationport"));
        this.logger = getToken(cXML, "log", "false").equalsIgnoreCase("true");
        this.cache = getToken(cXML, "cache", "false").equalsIgnoreCase("true");
        this.onlycache = getToken(cXML, "onlycache", "false").equalsIgnoreCase("true");

        // Se uno notes deve essere messo a 0
        this.timeout = Integer.parseInt(getToken(cXML, "timeout", "0"));
        this.maxclient = Integer.parseInt(getToken(cXML, "client", "10"));

        this.blockSize = Integer.parseInt(getToken(cXML, "blocksize", "64000"));

        // Se non ho la directory di LOG la creo
        if (logger) {
            try {
                java.io.File oFile = new java.io.File("log");
                if (!oFile.exists()) {
                    oFile.mkdir();
                }
            } catch (Throwable e) {
                log.error("Error creating log directory", e);
            }
        }

        if (cache) {
            try {
                java.io.File oFile = new java.io.File("cache");
                if (!oFile.exists()) {
                    oFile.mkdir();
                }
            } catch (Throwable e) {
                log.error("Error creating cache directory", e);
            }
        }

        log.info("Ready on [{}:{}] -> [{}:{}] TIMEOUT [{}]" + (cache ? " CACHE" : "") + (onlycache ? " ONLYCACHE" : ""), sourceAddress, sourcePort, destinationAddress, destinationPort, timeout);

    }

    private String getToken(String cXML, String cToken) {
        return getToken(cXML, cToken, "");
    }

    private String getToken(String cXML, String cToken, String cDefault) {
        String cRet = "";

        String cIni = "<" + cToken + ">";
        String cEnd = "</" + cToken + ">";
        int nPosIni = cXML.indexOf(cIni);
        int nPosEnd = cXML.indexOf(cEnd);

        if (nPosEnd > nPosIni) {
            cRet = cXML.substring(nPosIni + cIni.length(), nPosEnd);
        }

        if (cRet.length() == 0) {
            cRet = cDefault;
        }

        return cRet;
    }

    @Override
    public final void run() {
        Thread thread;
        try {                                  // port, maxrequest, address
            ServerSocket sock = new ServerSocket(sourcePort, maxclient, InetAddress.getByName(sourceAddress));
            while (true) {
                // Faccio partire il Thread
                Socket socket = sock.accept();

                // Metto anche il timeout ai socket
                if (timeout > 0) {
                    socket.setSoTimeout(timeout * 1000);
                }

                thread = new SockThread(this,
                        socket,
                        timeout,
                        destinationAddress,
                        destinationPort,
                        logger,
                        cache,
                        onlycache,
                        blockSize);

                thread.start();
            }
        } catch (BindException bind) {
            log.error("Address [{}:{}] already in use", sourceAddress, sourcePort);
        } catch (Throwable e) {
            log.error("Error in redirector from [{}] \t to [{}:{}]", sourcePort, destinationAddress, destinationPort);
            log.error("Full error", e);
        }
    }

    class SockThread extends Thread {

        private long threadNumber = 0;

        private final Socket socket;
        private final String cOutServer;
        private final int nPortTo;
        private final boolean logData;
        private final boolean cacheData;
        private final boolean onlyCache;
        private final int nSize;
        private SubSockThread sourceOutputToDestinationInputThread;
        private SubSockThread destinationOutputToSourceInputThread;

        public SockThread(PortRedirect server,
                Socket socket,
                long nTimeOut,
                String cOutServer,
                int nPortTo,
                boolean bLog,
                boolean bCache,
                boolean bOnlyCache,
                int nSize) {
            this.socket = socket;            
            this.cOutServer = cOutServer;
            this.nPortTo = nPortTo;
            this.logData = bLog;
            this.cacheData = bCache;
            this.onlyCache = bOnlyCache;
            this.nSize = nSize;

            threadNumber = THREADTOTAL.incrementAndGet();
            setName(threadNumber + "|" + sourcePort);
        }

        public synchronized void killProcess() {
            try {
                sourceOutputToDestinationInputThread.stop();
            } catch (ThreadDeath td) {
                log.error("ThreadDeath on killProcess [{}]", td.getMessage());
            } catch (Throwable e) {
                log.error("Throwable", e.getMessage());
            }

            try {
                destinationOutputToSourceInputThread.stop();
            } catch (ThreadDeath td) {
                log.error("ThreadDeath on killProcess [{}]", td.getMessage());
            } catch (Throwable e) {
                log.error("Throwable", e.getMessage());
            }
        }

        public void run() {
            if (logData) {
                log.info("[{}] new user [{}]", threadNumber, socket);
            }

            Socket socketOut = null;

            try {

                String cCacheDir = "";
                if (cacheData) {
                    try {
                        cCacheDir = "cache" + File.separatorChar + cOutServer + "." + nPortTo;
                        java.io.File oFile = new java.io.File(cCacheDir);
                        if (!oFile.exists()) {
                            oFile.mkdir();
                        }
                    } catch (Throwable e) {
                    }
                }

                InputStream sourceInputStream = socket.getInputStream();
                PrintStream sourceOutputStream = new PrintStream(socket.getOutputStream());

                InputStream destinationInputStream = null;
                PrintStream destinationOutputStream = null;
                if (!onlyCache) {
                    socketOut = new Socket(cOutServer, nPortTo);
                    destinationInputStream = socketOut.getInputStream();
                    destinationOutputStream = new PrintStream(socketOut.getOutputStream());
                }

                // S -> D
                sourceOutputToDestinationInputThread = new SubSockThread(this,
                        sourceOutputStream,
                        destinationInputStream,
                        destinationOutputStream,
                        sourceInputStream,
                        cOutServer + "-" + nPortTo + ".in" + threadNumber,
                        logData,
                        cacheData,
                        cCacheDir,
                        nSize);
                sourceOutputToDestinationInputThread.start();

                if (!cacheData) {
                    // D -> S
                    destinationOutputToSourceInputThread = new SubSockThread(this,
                            destinationOutputStream,
                            sourceInputStream,
                            sourceOutputStream,
                            destinationInputStream,
                            cOutServer + "-" + nPortTo + ".out" + threadNumber,
                            logData,
                            cacheData,
                            cCacheDir,
                            nSize);
                    destinationOutputToSourceInputThread.start();

                    while (destinationOutputToSourceInputThread.isAlive() && sourceOutputToDestinationInputThread.isAlive()) {
                        sleep(1000);
                    }
                } else {
                    while (sourceOutputToDestinationInputThread.isAlive()) {
                        sleep(1000);
                    }
                }

            } catch (Throwable e) {
                log.error("[{}] host:port [{}:{}]", threadNumber, cOutServer, nPortTo);
                log.error("Unknow error", e);
            }

            try {
                socket.close();
            } catch (Throwable e) {
                log.error("Error on socket.close", e);
            }

            try {
                // Se e' only cache non ho la socket out
                if (socketOut != null) {
                    socketOut.close();
                }
            } catch (Throwable e) {
                log.error("Error on socketOut.close", e);
            }

            if (logData) {
                log.info("[{}] disconnect", threadNumber);
            }

        }

        public class SubSockThread extends Thread {

            private final PrintStream sourceOutputStream;
            private final InputStream sourceInputStream;
            private final PrintStream destinationOutputStream;
            private final InputStream destinationInputStream;
            private final SockThread parentSockThread;
            private final String outputFileLog;
            private final boolean outputLog;
            private final boolean cache;
            private final String cacheDirectory;
            private final int nSize;

            public SubSockThread(SockThread parent,
                    PrintStream SO,
                    InputStream SI,
                    PrintStream OO,
                    InputStream OI,
                    String file,
                    boolean bLog,
                    boolean bCache,
                    String cCacheDir,
                    int nSize) {
                this.parentSockThread = parent;
                this.sourceOutputStream = SO;
                this.sourceInputStream = SI;
                this.destinationOutputStream = OO;
                this.destinationInputStream = OI;
                this.outputFileLog = file;
                this.outputLog = bLog;
                this.cache = bCache;
                this.cacheDirectory = cCacheDir;
                this.nSize = nSize;
                setName(outputFileLog);
            }

            @Override
            public void run() {
                // Attivo il sourceOutputToDestinationInputThread
                if (cache) {
                    runCache();
                } else {
                    runNormal();
                }

                // Distruggo tutti i processi
                parentSockThread.killProcess();
            }

            private void runNormal() {
                byte[] buffer = new byte[nSize];

                try {
                    RandomAccessFile RAF = null;
                    if (outputLog) {
                        RAF = new RandomAccessFile("log" + File.separatorChar + outputFileLog, "rw");
                    }

                    while (true) {
                        try {
                            // Va bene per tutti i protocolli ed e' abbastanza performante
                            int nPos = 0;
                            int nToRead = -1;
                            int c;
                            while (true) {
                                // Leggo un singolo byte
                                try {
                                    c = sourceInputStream.read();
                                    if (c == -1) {
                                        break;
                                    }
                                } catch (Throwable e) {
                                    break;
                                }

                                // Accodo il byte
                                buffer[nPos++] = (byte) c;

                                if (nToRead <= 0) {
                                    nToRead = sourceInputStream.available();
                                }

                                // Se arrivo a blockSize o se poi non ho piu' byte
                                // scrivo il buffer che ho in quel momento
                                if (nPos >= nSize || nToRead == 0) {
                                    sourceOutputStream.write(buffer, 0, nPos);
                                    if (outputLog) {
                                        RAF.write(buffer, 0, nPos);
                                    }

                                    nPos = 0;
                                }
                                nToRead--;

                            }

                            // Scrivo l'avanzo
                            sourceOutputStream.write(buffer, 0, nPos);
                            if (outputLog) {
                                RAF.write(buffer, 0, nPos);
                            }
                            nPos = 0;
                            break;

                        } catch (Throwable e) {
                            log.error("Error on runNormal", e);
                            break;
                        }
                    }

                    if (outputLog) {
                        RAF.close();
                    }

                } catch (ThreadDeath td) {
                    log.error("ThreadDeath on runNormal [{}]", td.getMessage());
                } catch (Throwable e) {
                    log.error("Error on runNormal file", e);
                }
            }

            private void runCache() {
                byte[] buffer = new byte[nSize];

                try {
                    RandomAccessFile RAF = null;
                    if (outputLog) {
                        RAF = new RandomAccessFile("log" + File.separatorChar + outputFileLog, "rw");
                    }

                    while (true) {
                        try {
                            // Va bene per tutti i protocolli ed e' abbastanza performante
                            int nPos = 0;
                            int nToRead = -1;
                            int c;
                            while (true) {
                                // Leggo un singolo byte
                                try {
                                    c = destinationInputStream.read();
                                    if (c == -1) {
                                        break;
                                    }
                                } catch (Throwable e) {
                                    break;
                                }

                                // Accodo il byte
                                buffer[nPos++] = (byte) c;

                                if (nToRead <= 0) {
                                    nToRead = destinationInputStream.available();
                                }

                                // Se arrivo a blockSize o se poi non ho piu' byte
                                // scrivo il buffer che ho in quel momento
                                if (nPos >= nSize || nToRead == 0) {
                                    // ------ LOG
                                    // if( logData ) RAF.write( buffer, 0, nPos );
                                    // ------ LOG

                                    // Provo a fare la cache
                                    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                                    crc.update(buffer, 0, nPos);
                                    String cCrc = cacheDirectory + File.separatorChar + crc.getValue();

                                    // Provo ad usare la cache
                                    boolean bUseCache = false;
                                    try {
                                        java.io.File oFile = new java.io.File(cCrc);
                                        if (oFile.exists()) {

                                            byte[] bufferSO;
                                            try (FileInputStream fInput = new FileInputStream(cCrc)) {
                                                bufferSO = new byte[fInput.available()];
                                                fInput.read(bufferSO);
                                            }

                                            sourceOutputStream.write(bufferSO);
                                            sourceOutputStream.flush();

                                            bUseCache = true;

                                        }
                                    } catch (Throwable e) {
                                    }

                                    // Se non ci riesco leggo il buffer
                                    if (!bUseCache) {

                                        try (RandomAccessFile oCache = new RandomAccessFile(cCrc, "rw")) {

                                            destinationOutputStream.write(buffer, 0, nPos);

                                            // Creo il buffer di lettura
                                            byte[] bufferSO = new byte[nSize];

                                            int cO;
                                            int nPosO = 0;
                                            int nToReadO = -1;
                                            while (true) {
                                                // Leggo un singolo byte
                                                try {
                                                    cO = sourceInputStream.read();
                                                    if (cO == -1) {
                                                        break;
                                                    }
                                                } catch (Throwable e) {
                                                    break;
                                                }

                                                // Accodo il byte
                                                bufferSO[nPosO++] = (byte) cO;

                                                // Vedo la disponibilita'
                                                if (nToReadO <= 0) {
                                                    nToReadO = sourceInputStream.available();
                                                }

                                                if (nPosO >= nSize || nToReadO == 0) {
                                                    sourceOutputStream.write(bufferSO, 0, nPosO);
                                                    oCache.write(bufferSO, 0, nPosO);
                                                    nPosO = 0;
                                                }
                                                nToReadO--;
                                            }
                                            sourceOutputStream.write(bufferSO, 0, nPosO);
                                            oCache.write(bufferSO, 0, nPosO);
                                        }
                                    }

                                    nPos = 0;
                                }
                                nToRead--;
                            }

                            break;

                        } catch (Throwable e) {
                            break;
                        }
                    }

                    if (outputLog) {
                        RAF.close();
                    }

                } catch (Throwable e) {
                    log.error("Error on runCache", e);
                }

            }

        }
    }
}
