/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector;

import it.baccan.sockredirector.pojo.ServerPojo;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matteo Baccan <matteo@baccan.it>
 */
public class SockThread extends Thread {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SockThread.class);
    /**
     *
     */
    private static final AtomicLong THREADTOTAL = new AtomicLong(0);

    private long threadNumber = 0;
    private final ServerPojo serverPojo;
    private final Socket socket;
    private SubSockThread sourceOutputToDestinationInputThread;
    private SubSockThread destinationOutputToSourceInputThread;

    public SockThread(Socket sock, ServerPojo server) {
        socket = sock;
        serverPojo = server;
        threadNumber = THREADTOTAL.incrementAndGet();
        setName("FROM:" + serverPojo.getSourcePort() + "|TO:" + serverPojo.getDestinationAddress() + ":" + serverPojo.getDestinationPort() + "|NUM:" + threadNumber);
    }

    public synchronized void killProcess() {
        try {
            sourceOutputToDestinationInputThread.interrupt();
        } catch (ThreadDeath td) {
            LOG.error("ThreadDeath on killProcess [{}]", td.getMessage());
        } catch (Throwable e) {
            LOG.error("Throwable", e);
        }

        try {
            destinationOutputToSourceInputThread.interrupt();
        } catch (ThreadDeath td) {
            LOG.error("ThreadDeath on killProcess [{}]", td.getMessage());
        } catch (Throwable e) {
            LOG.error("Throwable", e);
        }
    }

    @Override
    public void run() {
        if (serverPojo.isLogger()) {
            LOG.info("[{}] new user [{}]", threadNumber, socket);
        }

        Socket socketOut = null;

        try {

            String cCacheDir = "";
            if (serverPojo.isCache()) {
                try {
                    cCacheDir = "cache" + File.separatorChar + serverPojo.getDestinationAddress() + "." + serverPojo.getDestinationPort();
                    File oFile = new File(cCacheDir);
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
            if (!serverPojo.isOnlycache()) {
                socketOut = new Socket(serverPojo.getDestinationAddress(), serverPojo.getDestinationPort());
                destinationInputStream = socketOut.getInputStream();
                destinationOutputStream = new PrintStream(socketOut.getOutputStream());
            }

            // S -> D
            sourceOutputToDestinationInputThread = new SubSockThread(this,
                    sourceOutputStream,
                    destinationInputStream,
                    destinationOutputStream,
                    sourceInputStream,
                    serverPojo.getDestinationAddress() + "-" + serverPojo.getDestinationPort() + ".in" + threadNumber,
                    serverPojo.isLogger(),
                    serverPojo.isCache(),
                    cCacheDir,
                    serverPojo.getBlockSize());
            sourceOutputToDestinationInputThread.start();

            if (!serverPojo.isCache()) {
                // D -> S
                destinationOutputToSourceInputThread = new SubSockThread(this,
                        destinationOutputStream,
                        sourceInputStream,
                        sourceOutputStream,
                        destinationInputStream,
                        serverPojo.getDestinationAddress() + "-" + serverPojo.getDestinationPort() + ".out" + threadNumber,
                        serverPojo.isLogger(),
                        serverPojo.isCache(),
                        cCacheDir,
                        serverPojo.getBlockSize());
                destinationOutputToSourceInputThread.start();

                while (destinationOutputToSourceInputThread.isAlive() && sourceOutputToDestinationInputThread.isAlive()) {
                    sleep(1000);
                }
            } else {
                while (sourceOutputToDestinationInputThread.isAlive()) {
                    sleep(1000);
                }
            }

        } catch (InterruptedException e) {
            LOG.info("InterruptedException [{}]", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("[{}] host:port [{}:{}]", threadNumber, serverPojo.getDestinationAddress(), serverPojo.getDestinationPort());
            LOG.error("Unknow error", e);
        } finally {
            try {
                socket.close();
            } catch (Throwable e) {
                LOG.error("Error on socket.close", e);
            }
            try {
                // Se e' only cache non ho la socket out
                if (socketOut != null) {
                    socketOut.close();
                }
            } catch (Throwable e) {
                LOG.error("Error on socketOut.close", e);
            }
        }

        if (serverPojo.isLogger()) {
            LOG.info("[{}] disconnect", threadNumber);
        }

    }

}
