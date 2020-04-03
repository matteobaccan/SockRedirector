/*
 * Copyright (charRead) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector;

import it.baccan.sockredirector.util.SocketFlow;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matteo
 */
public class FlowThread extends Thread {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FlowThread.class);

    private final OutputStream sourceOutputStream;
    private final InputStream sourceInputStream;
    private final ServerSocketThread parentSockThread;
    private final String outputFileLog;
    private final boolean outputLog;
    private final int size;
    private final SocketFlow socketFlow;
    private final long readPause;
    private final long writePause;

    /**
     * FlowThread constructor.
     *
     * @param parent
     * @param outputStream
     * @param inputStream
     * @param file
     * @param bLog
     * @param size
     * @param socketFlow
     * @param readPause
     * @param writePause
     */
    public FlowThread(final ServerSocketThread parent,
            final OutputStream outputStream,
            final InputStream inputStream,
            final String file,
            final boolean bLog,
            final int size,
            final SocketFlow socketFlow,
            final long readPause,
            final long writePause) {
        this.parentSockThread = parent;
        this.sourceOutputStream = outputStream;
        this.sourceInputStream = inputStream;
        this.outputFileLog = file;
        this.outputLog = bLog;
        this.size = size;
        this.socketFlow = socketFlow;
        this.readPause = readPause;
        this.writePause = writePause;
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
        byte[] buffer = new byte[size];

        RandomAccessFile logFile = null;
        try {
            if (outputLog) {
                logFile = new RandomAccessFile("logs" + File.separatorChar + outputFileLog, "rw");
            }

            // Va bene per tutti i protocolli ed e' abbastanza performante
            int bufferPosition = 0;
            int bytesToRead = -1;
            int charRead;
            boolean firstPause = true;
            while (true) {
                if (firstPause) {
                    firstPause = false;
                    socketPause(readPause);
                }
                // Leggo un singolo byte
                try {
                    charRead = sourceInputStream.read();
                    if (charRead == -1) {
                        break;
                    }
                } catch (SocketException se) {
                    if ("Socket closed".equals(se.getMessage())) {
                        // Chiusura della connessione
                        LOG.info("[{}][{}] Socket closed", socketFlow.name(), parentSockThread.getThreadNumber());
                    } else {
                        LOG.error("[{}][{}] SocketException [{}]", socketFlow.name(), parentSockThread.getThreadNumber(), se.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    LOG.error("[{}][{}] readError [{}]", socketFlow.name(), parentSockThread.getThreadNumber(), e.getMessage());
                    break;
                }

                // Add byte to buffer
                buffer[bufferPosition++] = (byte) charRead;

                if (bytesToRead <= 0) {
                    bytesToRead = sourceInputStream.available();
                }

                // Write bytes if there arent new bytes to read or buffer is full
                if (bufferPosition >= size || bytesToRead == 0) {
                    socketPause(writePause);
                    sourceOutputStream.write(buffer, 0, bufferPosition);
                    if (logFile != null) {
                        logFile.write(buffer, 0, bufferPosition);
                    }
                    bufferPosition = 0;
                    firstPause = true;
                }
                bytesToRead--;
            }

            // Write last bytes
            socketPause(writePause);
            sourceOutputStream.write(buffer, 0, bufferPosition);
            if (logFile != null) {
                logFile.write(buffer, 0, bufferPosition);
            }
        } catch (ThreadDeath td) {
            LOG.error("[{}][{}] ThreadDeath on runNormal [{}]", socketFlow.name(), parentSockThread.getThreadNumber(), td.getMessage());
        } catch (IOException e) {
            LOG.error("[{}][{}] Error on runNormal [{}]", socketFlow.name(), parentSockThread.getThreadNumber(), e.getMessage());
        } finally {
            if (logFile != null) {
                try {
                    logFile.close();
                } catch (IOException iOException) {
                    LOG.error("[{}][{}] Error on closing log file [{}]", socketFlow.name(), parentSockThread.getThreadNumber(), iOException.getMessage());
                }
            }
        }
    }

    private void socketPause(long readPause) {
        if (readPause > 0) {
            LOG.info("[{}] pause of [{}] ms",socketFlow.name(), readPause);
            try {
                sleep(readPause);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
