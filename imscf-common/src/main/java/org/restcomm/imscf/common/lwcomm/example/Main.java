/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.imscf.common.lwcomm.example;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.service.FutureListener;
import org.restcomm.imscf.common.lwcomm.service.ListenableFuture;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;

/**
 * Main class for local standalone testing.
 * @author Miklos Pocsaji
 */
public final class Main {
    private static final Scanner SCANNER = new Scanner(System.in, "UTF-8");
    private static CountDownLatch cdl;
    private static final Logger LOGGER;
    static {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(ctx);
        ctx.reset();
        try {
            configurator.doConfigure("misc/logback.xml");
        } catch (JoranException e) {
            e.printStackTrace();
        }
        LOGGER = LoggerFactory.getLogger(Main.class);
    }

    /**
     * Types of standalone nodes. Used by setting "-Dlwcomm.name".
     * @author Miklos Pocsaji
     */
    enum NodeInstance {
        receiver1, receiver2, sender
    }

    private Main() {

    }

    public static void main(String[] args) throws Exception {
        NodeInstance ni = NodeInstance.valueOf(System.getProperty(Configuration.PARAMETER_LWCOMM_NAME));
        Configuration config = new LocalConfig(ni.toString());

        LwCommServiceProvider.init(config);

        LOGGER.info("waiting for heartbeats...");
        Thread.sleep(6000);

        if (ni == NodeInstance.sender) {
            int threads = 1;
            int count = 100;
            int delay = 100;
            int groupIdCount = 0;
            // try {
            // threads = Integer.parseInt(args[0]);
            // count = Integer.parseInt(args[1]);
            // delay = Integer.parseInt(args[2]);
            // } catch (Exception ex) {
            // LOGGER.error("Cannot parse all arguments, args: {}", Arrays.asList(args));
            // }

            boolean cont = true;
            while (cont) {
                threads = getInt("Threads: ");
                count = getInt("Count: ");
                delay = getInt("Delay: ");
                groupIdCount = getInt("Group id count: ");
                LOGGER.info("Threads: {}, count: {}, delay: {}, group id count: {}", threads, count, delay,
                        groupIdCount);
                if (threads < 0 || count < 0 || delay < 0) {
                    break;
                }
                cdl = new CountDownLatch(threads);
                for (int i = 0; i < threads; i++) {
                    new SenderThread(count, delay, groupIdCount).start();
                    Thread.sleep(delay);
                }
                cdl.await();
                LOGGER.info("FINISHED");
            }
        } else {
            System.out.println("Press ENTER to finish");
            System.in.read();
        }

        LwCommServiceProvider.getService().shutdown();

    }

    private static int getInt(String prompt) {
        int ret = -1;
        System.out.print(prompt);
        System.out.flush();
        try {
            ret = SCANNER.nextInt();
        } catch (Exception ex) {
            ret = -1;
        }
        return ret;
    }

    /**
     * Thread to send messages.
     * @author Miklos Pocsaji
     *
     */
    private static final class SenderThread extends Thread {

        private int count;
        private int delay;
        private int groupIdCount;

        private static final Random RANDOM = new Random();

        public SenderThread(int count, int delay, int groupIdCount) {
            this.count = count;
            this.delay = delay;
            this.groupIdCount = groupIdCount;
        }

        @Override
        public void run() {
            Listener listener = new Listener();
            TextMessage tm;
            if (groupIdCount > 0)
                tm = TextMessage.builder("sjdklfldsfkgjldfkgjldfskjgldsfkjglkdfgj").setGroupId(
                        String.valueOf(RANDOM.nextInt(groupIdCount))).create();
            else
                tm = TextMessage.builder("sladkjvlksdfjvlkdfjvldfjdflsjvdlsfjv").create();
            for (int i = 0; i < count; i++) {
                LwCommServiceProvider.getService().send("failover", tm).addListener(listener, null);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            cdl.countDown();
        }
    }

    /**
     * Listener which is invoked when sending a message is done.
     * @author Miklos Pocsaji
     */
    private static final class Listener implements FutureListener<SendResult> {
        @Override
        public void done(ListenableFuture<SendResult> listenableFuture) {
            SendResult sr;
            try {
                sr = listenableFuture.get();
                if (sr.getType() != SendResult.Type.SUCCESS) {
                    LOGGER.error("Error sending message: {}", sr);
                } else {
                    LOGGER.debug("Message sent successfully");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
