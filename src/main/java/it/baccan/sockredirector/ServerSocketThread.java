/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or https://www.gnu.org/licenses/gpl-3.0.html.
 *
 */
package it.baccan.sockredirector;

import it.baccan.sockredirector.pojo.ServerPojo;
import it.baccan.sockredirector.util.SocketFlow;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Matteo Baccan
 */
@Slf4j
public class ServerSocketThread extends Thread {

    private static final AtomicLong THREADTOTAL = new AtomicLong(0);

    @Getter
    private long threadNumber = 0;
    @Getter
    private final ServerPojo serverPojo;
    private final Socket socketIn;
    @Getter
    private FlowThread sourceOutputToDestinationInputThread;
    @Getter
    private FlowThread destinationOutputToSourceInputThread;

    /**
     * @param sock
     * @param server
     */
    public ServerSocketThread(Socket sock, ServerPojo server) {
        socketIn = sock;
        serverPojo = server;
        threadNumber = THREADTOTAL.incrementAndGet();
        setName(
                "FROM:"
                + serverPojo.getSourcePort()
                + "|TO:"
                + serverPojo.getDestinationAddress()
                + ":"
                + serverPojo.getDestinationPort());
    }

    /**
     * Interrupt the two subthreads.
     */
    public synchronized void killProcess() {
        log.info("Kill source thread [{}] from thread [{}]", sourceOutputToDestinationInputThread.getId(), this.getId());
        sourceOutputToDestinationInputThread.stopThread();
        log.info("Kill destination thread [{}] from thread [{}]", destinationOutputToSourceInputThread.getId(), this.getId());
        destinationOutputToSourceInputThread.stopThread();
    }

    /**
     * Run socket server.
     */
    @Override
    public void run() {
        if (getServerPojo().isLogger()) {
            log.info("new thread [{}] on connection  [{}]", threadNumber, socketIn);
        }

        try (Socket socketOut
                = new Socket(
                        getServerPojo().getDestinationAddress(),
                        getServerPojo().getDestinationPort())) {

            // S -> D
            sourceOutputToDestinationInputThread
                    = new FlowThread(
                            this,
                            socketIn.getOutputStream(),
                            socketOut.getInputStream(),
                            getServerPojo().getDestinationAddress()
                            + "-"
                            + getServerPojo().getDestinationPort()
                            + ".in"
                            + threadNumber,
                            getServerPojo().isLogger(),
                            getServerPojo().getBlockSize(),
                            SocketFlow.OUTBOUND,
                            getServerPojo().getOutReadWait(),
                            getServerPojo().getOutWriteWait());
            sourceOutputToDestinationInputThread.start();

            log.info("Start source thread [{}] from thread [{}]", sourceOutputToDestinationInputThread.getId(), this.getId());

            // D -> S
            destinationOutputToSourceInputThread
                    = new FlowThread(
                            this,
                            socketOut.getOutputStream(),
                            socketIn.getInputStream(),
                            getServerPojo().getDestinationAddress()
                            + "-"
                            + getServerPojo().getDestinationPort()
                            + ".out"
                            + threadNumber,
                            getServerPojo().isLogger(),
                            getServerPojo().getBlockSize(),
                            SocketFlow.INBOUND,
                            getServerPojo().getInReadWait(),
                            getServerPojo().getInWriteWait());
            destinationOutputToSourceInputThread.start();

            log.info("Start destination thread [{}] from thread [{}]", destinationOutputToSourceInputThread.getId(), this.getId());

            while (destinationOutputToSourceInputThread.isAlive()
                    && sourceOutputToDestinationInputThread.isAlive()) {
                sleep(1000);
                // Proviamo a killare in modo random il thread
                if (getServerPojo().getRandomKill() > 0) {
                    var random = new Random();
                    if (random.nextInt() % getServerPojo().getRandomKill() == 0) {
                        log.info("Kill random of thread [{}]", this.getId());
                        killProcess();
                    }
                }
            }

        } catch (InterruptedException e) {
            log.info("InterruptedException [{}]", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(
                    "[{}] host:port [{}:{}]",
                    threadNumber,
                    getServerPojo().getDestinationAddress(),
                    getServerPojo().getDestinationPort());
            log.error("Unknown error", e);
        } finally {
            // Kill all sub process when one thread exit
            killProcess();

            try {
                socketIn.close();
            } catch (IOException e) {
                log.error("Error on socket.close", e);
            }
        }

        if (getServerPojo().isLogger()) {
            log.info("end thread [{}] on connection  [{}]", threadNumber, socketIn);
        }
    }

}
