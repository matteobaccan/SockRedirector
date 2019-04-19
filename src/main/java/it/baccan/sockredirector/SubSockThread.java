/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 * 
 */
package it.baccan.sockredirector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matteo
 */
public class SubSockThread extends Thread {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SubSockThread.class);

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
                    LOG.error("Error on runNormal", e);
                    break;
                }
            }

            if (outputLog) {
                RAF.close();
            }

        } catch (ThreadDeath td) {
            LOG.error("ThreadDeath on runNormal [{}]", td.getMessage());
        } catch (Throwable e) {
            LOG.error("Error on runNormal file", e);
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
                        } catch (IOException e) {
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
                            CRC32 crc = new CRC32();
                            crc.update(buffer, 0, nPos);
                            String cCrc = cacheDirectory + File.separatorChar + crc.getValue();

                            // Provo ad usare la cache
                            boolean bUseCache = false;
                            try {
                                File oFile = new File(cCrc);
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
                            } catch (IOException e) {
                                LOG.error("Error using cache [{}]", cCrc);
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

            if (RAF != null) {
                RAF.close();
            }

        } catch (Throwable e) {
            LOG.error("Error on runCache", e);
        }

    }

}
