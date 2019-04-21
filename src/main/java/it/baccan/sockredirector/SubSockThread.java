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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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

    private final OutputStream sourceOutputStream;
    private final InputStream sourceInputStream;
    private final SockThread parentSockThread;
    private final String outputFileLog;
    private final boolean outputLog;
    private final int nSize;

    /**
     *
     * @param parent
     * @param SO
     * @param SI
     * @param file
     * @param bLog
     * @param nSize
     */
    public SubSockThread(SockThread parent,
            OutputStream SO,
            InputStream SI,
            String file,
            boolean bLog,
            int nSize) {
        this.parentSockThread = parent;
        this.sourceOutputStream = SO;
        this.sourceInputStream = SI;
        this.outputFileLog = file;
        this.outputLog = bLog;
        this.nSize = nSize;
        setName(outputFileLog);
    }

    @Override
    public void run() {
        // Attivo il sourceOutputToDestinationInputThread
        runNormal();

        // Distruggo tutti i processi
        parentSockThread.killProcess();
    }

    private void runNormal() {
        byte[] buffer = new byte[nSize];

        try {
            RandomAccessFile logFile = null;
            if (outputLog) {
                logFile = new RandomAccessFile("logs" + File.separatorChar + outputFileLog, "rw");
            }

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
                            logFile.write(buffer, 0, nPos);
                        }
                        nPos = 0;
                    }
                    nToRead--;

                }

                // Scrivo l'avanzo
                sourceOutputStream.write(buffer, 0, nPos);
                if (outputLog) {
                    logFile.write(buffer, 0, nPos);
                }
                nPos = 0;
            } catch (Throwable e) {
                LOG.error("Error on runNormal", e);
            }

            if (outputLog) {
                logFile.close();
            }

        } catch (ThreadDeath td) {
            LOG.error("ThreadDeath on runNormal [{}]", td.getMessage());
        } catch (Throwable e) {
            LOG.error("Error on runNormal file", e);
        }
    }

}
