/*
 * Copyright (charRead) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or https://www.gnu.org/licenses/gpl-3.0.html.
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
import lombok.extern.slf4j.Slf4j;

/**
 * @author Matteo Baccan
 */
@Slf4j
public class FlowThread extends Thread {

    private boolean exitThread = false;
    private final OutputStream sourceOutputStream;
    private final InputStream sourceInputStream;
    private final ServerSocketThread parentSockThread;
    private final String outputFileLog;
    private final boolean outputLog;
    private final int size;
    private final SocketFlow socketFlow;
    private long readPause;
    private long writePause;

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
    public FlowThread(
            final ServerSocketThread parent,
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

    /**
     * Execute socket Thread.
     */
    @Override
    public void run() {
        log.info("Ini run thread [{}]", this.getId());
        // Attivo il sourceOutputToDestinationInputThread
        runNormal();

        // Distruggo tutti i processi
        log.info("End run thread [{}]", this.getId());
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
            while (!exitThread) {
                if (firstPause) {
                    firstPause = false;
                    socketPause(getReadPause());
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
                        log.info(
                                "[{}][{}] Socket closed",
                                getSocketFlow().name(),
                                getParentSockThread().getThreadNumber());
                    } else {
                        log.error(
                                "[{}][{}] SocketException [{}]",
                                getSocketFlow().name(),
                                getParentSockThread().getThreadNumber(),
                                se.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    log.error(
                            "[{}][{}] readError [{}]",
                            getSocketFlow().name(),
                            getParentSockThread().getThreadNumber(),
                            e.getMessage());
                    break;
                }

                // Add byte to buffer
                buffer[bufferPosition++] = (byte) charRead;

                if (bytesToRead <= 0) {
                    bytesToRead = sourceInputStream.available();
                }

                // Write bytes if there are not new bytes to read or buffer is full
                if (bufferPosition >= size || bytesToRead == 0) {
                    socketPause(getWritePause());
                    logData(logFile, buffer, bufferPosition);
                    bufferPosition = 0;
                    firstPause = true;
                }
                bytesToRead--;
            }

            // Write last bytes
            socketPause(getWritePause());
            logData(logFile, buffer, bufferPosition);
        } catch (ThreadDeath td) {
            log.error(
                    "[{}][{}] ThreadDeath on runNormal [{}]",
                    getSocketFlow().name(),
                    getParentSockThread().getThreadNumber(),
                    td.getMessage());
        } catch (IOException e) {
            log.error(
                    "[{}][{}] Error on runNormal [{}]",
                    getSocketFlow().name(),
                    getParentSockThread().getThreadNumber(),
                    e.getMessage());
        } finally {
            if (logFile != null) {
                try {
                    logFile.close();
                } catch (IOException iOException) {
                    log.error(
                            "[{}][{}] Error on closing log file [{}]",
                            getSocketFlow().name(),
                            getParentSockThread().getThreadNumber(),
                            iOException.getMessage());
                }
            }
        }
    }

    private void socketPause(long readPause) {
        if (readPause > 0) {
            try {
                sleep(readPause);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @return the readPause
     */
    public long getReadPause() {
        return readPause;
    }

    /**
     * @param readPause the readPause to set
     */
    public void setReadPause(long readPause) {
        this.readPause = readPause;
    }

    /**
     * @return the writePause
     */
    public long getWritePause() {
        return writePause;
    }

    /**
     * @param writePause the writePause to set
     */
    public void setWritePause(long writePause) {
        this.writePause = writePause;
    }

    /**
     * @return the parentSockThread
     */
    public ServerSocketThread getParentSockThread() {
        return parentSockThread;
    }

    /**
     * @return the socketFlow
     */
    public SocketFlow getSocketFlow() {
        return socketFlow;
    }

    private void logData(
            final RandomAccessFile logFile, final byte[] buffer, final int bufferPosition)
            throws IOException {
        sourceOutputStream.write(buffer, 0, bufferPosition);
        if (logFile != null) {
            logFile.write(buffer, 0, bufferPosition);
        }
    }

    /**
     * Stop current thread.
     */
    public void stopThread() {
        log.info("Exit from thread [{}]", this.getId());
        exitThread = true;
        try {
            sourceInputStream.close();
        } catch (IOException ex) {
            log.error("stopThread close socket [{}]", ex.getMessage());
        }
    }
}
