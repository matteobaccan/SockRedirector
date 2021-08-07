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
import it.baccan.sockredirector.util.SocketFlow;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Matteo Baccan */
public class ServerSocketThread extends Thread {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(ServerSocketThread.class);
  /** */
  private static final AtomicLong THREADTOTAL = new AtomicLong(0);

  private long threadNumber = 0;
  private final ServerPojo serverPojo;
  private final Socket socketIn;
  private FlowThread sourceOutputToDestinationInputThread;
  private FlowThread destinationOutputToSourceInputThread;

  /**
   * Current htread number.
   *
   * @return
   */
  public long getThreadNumber() {
    return threadNumber;
  }

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
            + serverPojo.getDestinationPort()
            + "|NUM:"
            + threadNumber);
  }

  /** Interrupt the two subthreads. */
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
    if (getServerPojo().isLogger()) {
      LOG.info("[{}] new user [{}]", threadNumber, socketIn);
    }

    try (Socket socketOut =
        new Socket(getServerPojo().getDestinationAddress(), getServerPojo().getDestinationPort())) {

      // S -> D
      sourceOutputToDestinationInputThread =
          new FlowThread(
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

      // D -> S
      destinationOutputToSourceInputThread =
          new FlowThread(
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

      while (destinationOutputToSourceInputThread.isAlive()
          && sourceOutputToDestinationInputThread.isAlive()) {
        sleep(1000);
      }

    } catch (InterruptedException e) {
      LOG.info("InterruptedException [{}]", e.getMessage());
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOG.error(
          "[{}] host:port [{}:{}]",
          threadNumber,
          getServerPojo().getDestinationAddress(),
          getServerPojo().getDestinationPort());
      LOG.error("Unknow error", e);
    } finally {
      try {
        socketIn.close();
      } catch (IOException e) {
        LOG.error("Error on socket.close", e);
      }
    }

    if (getServerPojo().isLogger()) {
      LOG.info("[{}] disconnect", threadNumber);
    }
  }

  /** @return the serverPojo */
  public ServerPojo getServerPojo() {
    return serverPojo;
  }
}
