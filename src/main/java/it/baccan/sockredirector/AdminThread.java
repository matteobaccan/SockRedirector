/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or https://www.gnu.org/licenses/gpl-3.0.html.
 *
 */
package it.baccan.sockredirector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin thread.
 *
 * @author Matteo Baccan
 * @version 1.0
 */
@Slf4j
public class AdminThread extends Thread {

    /**
     * Admin thread constructor.
     */
    public AdminThread() {
        super();
        setName("AdminThread");
    }

    /**
     * Run admin thread.
     */
    @Override
    public final void run() {
        try {

            var input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = input.readLine();

                if (!line.isEmpty()) {
                    log.info("Admin> [{}]", line);
                }

                line = line.trim();

                int nSpace = line.indexOf(' ');
                String command;
                String parameter = "";

                if (nSpace != -1) {
                    command = line.substring(0, nSpace).trim();
                    parameter = line.substring(nSpace).trim();
                } else {
                    command = line;
                }

                if (command.equalsIgnoreCase("help")) {
                    log.info("");
                    log.info("help                      - this help");
                    log.info("exit                      - exit program");
                    log.info("thread [filter]           - thread list");
                    log.info("kill <nth> [... [<nth>]]  - kill nth thread");
                    log.info("pause <nth> <readPause> <writePause> - set pause on nth thread");
                    log.info("");
                } else if (command.equalsIgnoreCase("exit")) {
                    Runtime.getRuntime().halt(0);
                } else if (command.equalsIgnoreCase("thread")) {
                    log.info("");
                    dumpThread(parameter);
                    log.info("");
                } else if (command.equalsIgnoreCase("pause")) {
                    log.info("");
                    pauseThread(parameter);
                    log.info("");
                } else if (command.equalsIgnoreCase("kill")) {
                    log.info("");
                    String[] th = parameter.split(" ");
                    for (String t : th) {
                        kill(t);
                    }
                    log.info("");
                }
            }
        } catch (IOException e) {
            log.error("adminThread error", e);
        }
    }

    private void dumpThread(String filter) {
        // Ask all threads to JVM
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        threadSet.forEach(
                (Thread thread) -> {
                    String info = getThreadInfo(thread);
                    // If is in filter and the tread is not System
                    if ((filter.isEmpty() || info.contains(filter))
                    && (thread.getThreadGroup() != null
                    && !"system".equals(thread.getThreadGroup().getName()))
                    && info.length() > 0) {
                        log.info(info);
                    }
                });
    }

    private void pauseThread(String parameter) {
        String[] th = parameter.split(" ");

        // Ask all threads to JVM
        if (th.length > 0) {
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            threadSet.forEach(
                    (Thread thread) -> {
                        if (thread instanceof FlowThread) {
                            FlowThread flowThread = (FlowThread) thread;

                            // If is in filter and the tread is not System
                            if ((th[0].isEmpty() || ("" + thread.getId()).equals(th[0]))) {

                                try {
                                    if (th.length > 1) {
                                        flowThread.setReadPause(Long.parseLong(th[1]));
                                    }
                                } catch (NumberFormatException numberFormatException) {
                                    log.error("Wrong number [{}]", th[1]);
                                }
                                try {
                                    if (th.length > 2) {
                                        flowThread.setWritePause(Long.parseLong(th[2]));
                                    }
                                } catch (NumberFormatException numberFormatException) {
                                    log.error("Wrong number [{}]", th[2]);
                                }
                            }
                        }
                    });
        }
    }

    private void kill(final String cThread) {
        // Ask all threads to JVM
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        AtomicBoolean bKill = new AtomicBoolean(false);
        threadSet.forEach(
                (Thread thread) -> {
                    try {
                        if (cThread.equals("" + thread.getId())) {
                            bKill.set(true);
                            log.info("Try to kill ID[{}]", cThread);
                            thread.interrupt();
                            log.info("Thread killed");
                        }
                    } catch (ThreadDeath td) {
                        log.error("ThreadDeath on admin kill [{}]", td.getMessage());
                    } catch (Exception exception) {
                        log.error("Exception on admin kill", exception);
                    }
                });

        if (!bKill.get()) {
            log.info("Thread not found");
        }
    }

    private String getThreadInfo(final Thread thread) {
        StringBuilder sb = new StringBuilder(128);
        try {
            if (thread instanceof ServerSocketThread) {
                ServerSocketThread serverSocketThread = (ServerSocketThread) thread;

                sb.append("|ID ");
                sb.append(String.format("%1$5d", thread.getId()));
                sb.append("|");

                sb.append(padRight(serverSocketThread.getServerPojo().getSourceAddress() + ":" + serverSocketThread.getServerPojo().getSourcePort(), 30));
                sb.append("|ID ");
                sb.append(String.format("%1$5d", serverSocketThread.getSourceOutputToDestinationInputThread().getId()));
                sb.append("|");
                sb.append(serverSocketThread.getSourceOutputToDestinationInputThread().getSocketFlow().name());
                sb.append("|R PAUSE ");
                sb.append(padRight("" + serverSocketThread.getSourceOutputToDestinationInputThread().getReadPause(), 5));
                sb.append("|W PAUSE ");
                sb.append(padRight("" + serverSocketThread.getSourceOutputToDestinationInputThread().getWritePause(), 5));
                sb.append("|");
                sb.append(padRight(thread.getName(), 20));

                sb.append("|-|");
                sb.append(serverSocketThread.getDestinationOutputToSourceInputThread().getSocketFlow().name());
                sb.append("|ID ");
                sb.append(String.format("%1$5d", serverSocketThread.getDestinationOutputToSourceInputThread().getId()));
                sb.append("|R PAUSE ");
                sb.append(padRight("" + serverSocketThread.getDestinationOutputToSourceInputThread().getReadPause(), 5));
                sb.append("|W PAUSE ");
                sb.append(padRight("" + serverSocketThread.getDestinationOutputToSourceInputThread().getWritePause(), 5));
                sb.append("|");
                sb.append(padRight(thread.getName(), 20));
            }
        } catch (Exception exception) {
            log.error("getThreadInfo error", exception);
        }
        return sb.toString();
    }

    private String padRight(final String string, final int len) {
        return String.format("%-" + len + "s", string);
    }
}
