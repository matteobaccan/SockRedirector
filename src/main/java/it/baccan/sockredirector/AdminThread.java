/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector;

import it.baccan.sockredirector.util.SocketFlow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin thread.
 *
 * @author Matteo Baccan
 * @version 1.0
 */
public class AdminThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(AdminThread.class);

    /**
     * Admin thread constructor.
     */
    public AdminThread() {
        super();
        setName("AdminThread");
    }

    @Override
    public final void run() {
        try {

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = input.readLine();

                if (!line.isEmpty()) {
                    LOG.info("Admin> [{}]", line);
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
                    LOG.info("");
                    LOG.info("help                      - this help");
                    LOG.info("exit                      - exit program");
                    LOG.info("thread [filter]           - thread list");
                    LOG.info("kill <nth> [... [<nth>]]  - kill nth thread");
                    LOG.info("pause <nth> <readPause> <writePause> - set pause on nth thread");
                    LOG.info("");
                } else if (command.equalsIgnoreCase("exit")) {
                    Runtime.getRuntime().halt(0);
                } else if (command.equalsIgnoreCase("thread")) {
                    LOG.info("");
                    dumpThread(parameter);
                    LOG.info("");
                } else if (command.equalsIgnoreCase("pause")) {
                    LOG.info("");
                    pauseThread(parameter);
                    LOG.info("");
                } else if (command.equalsIgnoreCase("kill")) {
                    LOG.info("");
                    String[] th = parameter.split(" ");
                    for (String t : th) {
                        kill(t);
                    }
                    LOG.info("");
                }
            }
        } catch (IOException e) {
            LOG.error("adminThread error", e);
        }
    }

    private void dumpThread(String filter) {
        // Ask all threads to JVM
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        threadSet.forEach((Thread thread) -> {
            String info = getThreadInfo(thread);
            // If is in filter and the tread is not System
            if ((filter.isEmpty() || info.contains(filter))
                    && (thread.getThreadGroup() != null && !"system".equals(thread.getThreadGroup().getName()))
                    && info.length() > 0) {
                LOG.info(info);
            }
        });
    }

    private void pauseThread(String parameter) {
        String[] th = parameter.split(" ");

        // Ask all threads to JVM
        if (th.length > 0) {
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            threadSet.forEach((Thread thread) -> {
                String info = getThreadInfo(thread);
                // If is in filter and the tread is not System
                if ((th[0].isEmpty() || info.contains(th[0]))
                        && (thread.getThreadGroup() != null && !"system".equals(thread.getThreadGroup().getName()))
                        && thread instanceof FlowThread) {

                    FlowThread flowThread = (FlowThread) thread;
                    try {
                        if (th.length > 1) {
                            flowThread.setReadPause(Long.parseLong(th[1]));
                        }
                    } catch (NumberFormatException numberFormatException) {
                        LOG.error("Wrong number [{}]", th[1]);
                    }
                    try {
                        if (th.length > 2) {
                            flowThread.setWritePause(Long.parseLong(th[2]));
                        }
                    } catch (NumberFormatException numberFormatException) {
                        LOG.error("Wrong number [{}]", th[2]);
                    }

                }
            });
        }
    }

    private void kill(final String cThread) {
        // Ask all threads to JVM
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        AtomicBoolean bKill = new AtomicBoolean(false);
        threadSet.forEach((Thread thread) -> {
            try {
                if (cThread.equals("" + thread.getId())) {
                    bKill.set(true);
                    LOG.info("Try to kill ID[{}]", cThread);
                    thread.interrupt();
                    LOG.info("Thread killed");
                }
            } catch (ThreadDeath td) {
                LOG.error("ThreadDeath on admin kill [{}]", td.getMessage());
            } catch (Exception exception) {
                LOG.error("Exception on admin kill", exception);
            }
        });

        if (!bKill.get()) {
            LOG.info("Thread not found");
        }

    }

    private String getThreadInfo(final Thread thread) {
        StringBuilder sb = new StringBuilder(128);
        try {
            if (thread instanceof FlowThread) {
                FlowThread flowThread = (FlowThread) thread;
                sb.append(flowThread.getParentSockThread().getServerPojo().getSourceAddress());
                sb.append(":").append(flowThread.getParentSockThread().getServerPojo().getSourcePort());
                if (flowThread.getSocketFlow() == SocketFlow.OUTBOUND) {
                    sb.append("->");
                } else {
                    sb.append("<-");
                }
                sb.append(flowThread.getSocketFlow().name());
                sb.append(" readPause[");
                sb.append(flowThread.getReadPause());
                sb.append("]");
                sb.append(" writePause[");
                sb.append(flowThread.getWritePause());
                sb.append("]");

                sb.append(" ID[").append(String.format("%1$5d", thread.getId())).append("]");
                sb.append(" [").append((thread.getClass().getSimpleName() + "                    ").substring(0, 20)).append("]");
                sb.append(" STATE[").append(thread.getState()).append("]");
                sb.append(" PRIORITY[").append(thread.getPriority()).append("]");
                ThreadGroup threadGroup = thread.getThreadGroup();
                if (threadGroup != null) {
                    sb.append(" GROUP[").append(threadGroup.getName()).append("]");
                }
                if (thread.isDaemon()) {
                    sb.append(" [daemon]");
                }
                if (thread.isAlive()) {
                    sb.append(" [alive]");
                }
                if (thread.isInterrupted()) {
                    sb.append(" [interrupted]");
                }
                sb.append(" ").append(thread.getName());
            }
        } catch (Exception exception) {
            LOG.error("getThreadInfo error", exception);
        }
        return sb.toString();
    }
}
