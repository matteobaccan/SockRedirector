/*
 * Debug Thread
 *
 * Copyright 2001 Matteo Baccan <baccan@infomedia.it>
 * www - http://www.infomedia.it/artic/Baccan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA (or visit
 * their web site at http://www.gnu.org/).
 *
 */
package it.baccan.sockredirector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe generica di amministrazione del thread
 *
 * @author Matteo Baccan <matteo@baccan.it>
 * @version 1.0
 */
@Slf4j
public class AdminThread extends Thread {

    /**
     * Thread amministrativo di gestione processi.
     */
    public AdminThread() {
    }

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
