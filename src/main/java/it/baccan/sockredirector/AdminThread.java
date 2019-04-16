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
                String cCommand = "";
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
        // Risalgo al primo parent
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }

        Thread[] threads = new Thread[group.activeCount()];

        int threadCount = Math.min(group.enumerate(threads, true), threads.length);

        for (int i = 0; i < threadCount; i++) {
            String info = getThreadInfo(threads[i]);
            //int hashCode = System.identityHashCode(threads[i]);
            //log.info('[' + Integer.toHexString(hashCode) + "] " + info);
            if (filter.isEmpty()) {
                log.info(info);
            } else if (info.contains(filter)) {
                log.info(info);
            }
        }

    }

    private void kill(final String cThread) {
        // Risalgo al primo parent
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }

        Thread[] threads = new Thread[group.activeCount()];

        int threadCount = Math.min(group.enumerate(threads, true), threads.length);

        boolean bKill = false;
        for (int i = 0; i < threadCount; i++) {
            //String info = getThreadInfo(threads[i]);
            //int hashCode = System.identityHashCode(threads[i]);
            //String cTH = Integer.toHexString(hashCode);
            try {
                if (cThread.equals("" + threads[i].getId())) {
                    bKill = true;

                    log.info("Try to kill " + cThread);
                    threads[i].stop();
                    log.info("Thread killed");
                }
            } catch (ThreadDeath td) {
                log.error("ThreadDeath on admin kill [{}]", td.getMessage());
            } catch (Exception exception) {
                log.error("Exception on admin kill", exception);
            }
        }

        if (!bKill) {
            log.info("Thread not found");
        }

    }

    private String getThreadInfo(final Thread thread) {
        StringBuilder sb = new StringBuilder(128);
        try {
            sb.append("[").append(thread.getId()).append("]");
            sb.append("[").append(thread.getState()).append("]");
            sb.append(thread);
            if (thread.isDaemon()) {
                sb.append(", daemon");
            }
            if (thread.isAlive()) {
                sb.append(", alive");
            }
            if (thread.isInterrupted()) {
                sb.append(", interrupted");
            }
            sb.append(", priority = ").append(thread.getPriority());
            sb.append(", name = ").append(thread.getName());
        } catch (Exception exception) {
            log.error("getThreadInfo error", exception);
        }
        return sb.toString();
    }
}
