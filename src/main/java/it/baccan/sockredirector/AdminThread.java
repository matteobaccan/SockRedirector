/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
 /*
 * Debug Thread
 */
package it.baccan.sockredirector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe generica di amministrazione del thread
 *
 * @author Matteo Baccan <matteo@baccan.it>
 * @version 1.0
 */
public class AdminThread extends Thread {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminThread.class);

    @Override
    public final void run() {
        try {

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String cLine = input.readLine();
                cLine = cLine.trim();

                int nSpace = cLine.indexOf(' ');
                String cCommand;
                String cParam = "";

                if (nSpace != -1) {
                    cCommand = cLine.substring(0, nSpace).trim();
                    cParam = cLine.substring(nSpace).trim();
                } else {
                    cCommand = cLine;
                }

                if (cCommand.equalsIgnoreCase("help")) {
                    log.info("");
                    log.info("help                      - this help");
                    log.info("exit                      - exit program");
                    log.info("thread [filter]           - thread list");
                    log.info("kill <nth> [... [<nth>]]  - kill nth thread");
                    log.info("");
                } else if (cCommand.equalsIgnoreCase("exit")) {
                    System.exit(0);
                } else if (cCommand.equalsIgnoreCase("thread")) {
                    log.info("");
                    dumpThread(cParam);
                    log.info("");
                } else if (cCommand.equalsIgnoreCase("kill")) {
                    log.info("");
                    String[] th = cParam.split(" ");
                    for (String t : th) {
                        kill(t);
                    }
                    log.info("");
                }
            }
        } catch (Throwable e) {
            log.error("adminThread error", e);
        }
    }

    private void dumpThread(String filter) {
        // Ask all threads to JVM
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        threadSet.forEach((Thread thread) -> {
            String info = getThreadInfo(thread);
            if (filter.isEmpty() || info.contains(filter)) {
                log.info(info);
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
            sb.append(" ID[").append(thread.getId()).append("]");
            sb.append(" ").append(thread.getName());
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
        } catch (Exception exception) {
            log.error("getThreadInfo error", exception);
        }
        return sb.toString();
    }
}
