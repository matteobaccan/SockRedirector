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
    private final Socket socketIn;
    private SubSockThread sourceOutputToDestinationInputThread;
    private SubSockThread destinationOutputToSourceInputThread;

    /**
     *
     * @param sock
     * @param server
     */
    public SockThread(Socket sock, ServerPojo server) {
        socketIn = sock;
        serverPojo = server;
        threadNumber = THREADTOTAL.incrementAndGet();
        setName("FROM:" + serverPojo.getSourcePort() + "|TO:" + serverPojo.getDestinationAddress() + ":" + serverPojo.getDestinationPort() + "|NUM:" + threadNumber);
    }

    /**
     * Interrupt the two subthreads.
     */
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
            LOG.info("[{}] new user [{}]", threadNumber, socketIn);
        }

        try (Socket socketOut = new Socket(serverPojo.getDestinationAddress(), serverPojo.getDestinationPort())) {

            // S -> D
            sourceOutputToDestinationInputThread = new SubSockThread(this,
                    socketIn.getOutputStream(),
                    socketOut.getInputStream(),
                    serverPojo.getDestinationAddress() + "-" + serverPojo.getDestinationPort() + ".in" + threadNumber,
                    serverPojo.isLogger(),
                    serverPojo.getBlockSize());
            sourceOutputToDestinationInputThread.start();

            // D -> S
            destinationOutputToSourceInputThread = new SubSockThread(this,
                    socketOut.getOutputStream(),
                    socketIn.getInputStream(),
                    serverPojo.getDestinationAddress() + "-" + serverPojo.getDestinationPort() + ".out" + threadNumber,
                    serverPojo.isLogger(),
                    serverPojo.getBlockSize());
            destinationOutputToSourceInputThread.start();

            while (destinationOutputToSourceInputThread.isAlive() && sourceOutputToDestinationInputThread.isAlive()) {
                sleep(1000);
            }

        } catch (InterruptedException e) {
            LOG.info("InterruptedException [{}]", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("[{}] host:port [{}:{}]", threadNumber, serverPojo.getDestinationAddress(), serverPojo.getDestinationPort());
            LOG.error("Unknow error", e);
        } finally {
            try {
                socketIn.close();
            } catch (Throwable e) {
                LOG.error("Error on socket.close", e);
            }
        }

        if (serverPojo.isLogger()) {
            LOG.info("[{}] disconnect", threadNumber);
        }

    }

}
