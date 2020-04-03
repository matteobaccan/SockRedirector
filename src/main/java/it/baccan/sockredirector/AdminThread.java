/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector;

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
                String cCommand;
                String cParam = "";

                if (nSpace != -1) {
                    cCommand = line.substring(0, nSpace).trim();
                    cParam = line.substring(nSpace).trim();
                } else {
                    cCommand = line;
                }

                if (cCommand.equalsIgnoreCase("help")) {
                    LOG.info("");
                    LOG.info("help                      - this help");
                    LOG.info("exit                      - exit program");
                    LOG.info("thread [filter]           - thread list");
                    LOG.info("kill <nth> [... [<nth>]]  - kill nth thread");
                    LOG.info("");
                } else if (cCommand.equalsIgnoreCase("exit")) {
                    Runtime.getRuntime().halt(0);
                } else if (cCommand.equalsIgnoreCase("thread")) {
                    LOG.info("");
                    dumpThread(cParam);
                    LOG.info("");
                } else if (cCommand.equalsIgnoreCase("kill")) {
                    LOG.info("");
                    String[] th = cParam.split(" ");
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
                    && (thread.getThreadGroup() != null && !"system".equals(thread.getThreadGroup().getName()))) {
                LOG.info(info);
            }
        });
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
            sb.append(" ID[").append(String.format("%1$5d", thread.getId())).append("]");
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
        } catch (Exception exception) {
            LOG.error("getThreadInfo error", exception);
        }
        return sb.toString();
    }
}
