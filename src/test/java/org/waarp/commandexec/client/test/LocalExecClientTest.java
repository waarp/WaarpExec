/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.commandexec.client.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import org.waarp.commandexec.client.LocalExecClientHandler;
import org.waarp.commandexec.client.LocalExecClientInitializer;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;

/**
 * LocalExec client.
 *
 * This class is an example of client.
 *
 * On a bi-core Centrino2 vPro: 18/s in 50 sequential, 30/s in 10 threads with 50 sequential<br>
 * On a quad-core i7: 29/s in 50 sequential, 187/s in 10 threads with 50 sequential
 */
public class LocalExecClientTest extends Thread {

    static int nit = 50;
    static int nth = 10;
    static String command = "/opt/R66/testexec.sh";
    static int port = 9999;
    static InetSocketAddress address;

    static LocalExecResult result;
    static int ok = 0;
    static int ko = 0;
    static AtomicInteger atomicInteger = new AtomicInteger();

    static EventLoopGroup workerGroup = new NioEventLoopGroup();
    // Configure the client.
    static Bootstrap bootstrap;
    // Configure the pipeline factory.
    static LocalExecClientInitializer localExecClientInitializer;

    /**
     * Test & example main
     * 
     * @param args
     *            ignored
     * @throws Exception
     */
    public static void main(String[] aregs) throws Exception {
        WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(
                WaarpLogLevel.WARN));
        InetAddress addr;
        byte[] loop = { 127, 0, 0, 1 };
        try {
            addr = InetAddress.getByAddress(loop);
        } catch (UnknownHostException e) {
            return;
        }
        address = new InetSocketAddress(addr, port);

        // Configure the client.
        bootstrap = new Bootstrap();
        WaarpNettyUtil.setBootstrap(bootstrap, workerGroup, 30000);
        // Configure the pipeline factory.
        localExecClientInitializer = new LocalExecClientInitializer();
        bootstrap.handler(localExecClientInitializer);

        try {
            // Parse options.
            LocalExecClientTest client = new LocalExecClientTest();
            // run once
            long first = System.currentTimeMillis();
            if (client.connect()) {
                client.runOnce();
                client.disconnect();
            }
            long second = System.currentTimeMillis();
            // print time for one exec
            System.err.println("1=Total time in ms: " + (second - first) + " or " + (1 * 1000 / (second - first))
                    + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
            // Now run multiple within one thread
            first = System.currentTimeMillis();
            for (int i = 0; i < nit; i++) {
                if (client.connect()) {
                    client.runOnce();
                    client.disconnect();
                }
            }
            second = System.currentTimeMillis();
            // print time for one exec
            System.err.println(nit + "=Total time in ms: " + (second - first) + " or "
                    + (nit * 1000 / (second - first)) + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
            // Now run multiple within multiple threads
            // Create multiple threads
            ExecutorService executorService = Executors.newFixedThreadPool(nth);
            first = System.currentTimeMillis();
            // Starts all thread with a default number of execution
            for (int i = 0; i < nth; i++) {
                executorService.submit(new LocalExecClientTest());
            }
            Thread.sleep(500);
            executorService.shutdown();
            while (!executorService.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                Thread.sleep(50);
            }
            second = System.currentTimeMillis();

            // print time for one exec
            System.err.println((nit * nth) + "=Total time in ms: " + (second - first) + " or "
                    + (nit * nth * 1000 / (second - first)) + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;

            // run once
            first = System.currentTimeMillis();
            if (client.connect()) {
                client.runFinal();
                client.disconnect();
            }
            second = System.currentTimeMillis();
            // print time for one exec
            System.err.println("1=Total time in ms: " + (second - first) + " or " + (1 * 1000 / (second - first))
                    + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
        } finally {
            // Shut down all thread pools to exit.
            workerGroup.shutdownGracefully();
            localExecClientInitializer.releaseResources();
        }
    }

    /**
     * Simple constructor
     */
    public LocalExecClientTest() {
    }

    private Channel channel;

    /**
     * Run method for thread
     */
    public void run() {
        if (connect()) {
            for (int i = 0; i < nit; i++) {
                this.runOnce();
            }
            disconnect();
        }
    }

    /**
     * Connect to the Server
     */
    private boolean connect() {
        // Start the connection attempt.
        ChannelFuture future = bootstrap.connect(address);

        // Wait until the connection attempt succeeds or fails.
        try {
            channel = future.await().sync().channel();
        } catch (InterruptedException e) {
        }
        if (!future.isSuccess()) {
            System.err.println("Client Not Connected");
            future.cause().printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Disconnect from the server
     */
    private void disconnect() {
        // Close the connection. Make sure the close operation ends because
        // all I/O operations are asynchronous in Netty.
        try {
            ChannelFuture closeFuture = WaarpSslUtility.closingSslChannel(channel);
            closeFuture.await(30000);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Run method both for not threaded execution and threaded execution
     */
    public void runOnce() {
        // Initialize the command context
        LocalExecClientHandler clientHandler =
                (LocalExecClientHandler) channel.pipeline().last();
        // Command to execute
        String line = command + " " + atomicInteger.incrementAndGet();
        clientHandler.initExecClient(0, line);
        // Wait for the end of the exec command
        LocalExecResult localExecResult = clientHandler.waitFor(10000);
        int status = localExecResult.getStatus();
        if (status < 0) {
            System.err.println(line + " Status: " + status + "\tResult: " +
                    localExecResult.getResult());
            ko++;
        } else {
            ok++;
            result = localExecResult;
        }
    }

    /**
     * Run method for closing Server
     */
    private void runFinal() {
        // Initialize the command context
        LocalExecClientHandler clientHandler =
                (LocalExecClientHandler) channel.pipeline().last();
        // Command to execute
        clientHandler.initExecClient(-1000, "stop");
        // Wait for the end of the exec command
        LocalExecResult localExecResult = clientHandler.waitFor(10000);
        int status = localExecResult.getStatus();
        if (status < 0) {
            System.err.println("Shutdown Status: " + status + "\nResult: " +
                    localExecResult.getResult());
            ko++;
        } else {
            ok++;
            result = localExecResult;
        }
    }
}
