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
package org.waarp.commandexec.ssl.client.test;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.waarp.commandexec.ssl.client.LocalExecSslClientHandler;
import org.waarp.commandexec.ssl.client.LocalExecSslClientPipelineFactory;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * LocalExecSsl client.
 *
 * This class is an example of client.
 *
 * No client authentication On a bi-core Centrino2 vPro: 5/s in 50 sequential, 29/s in 10 threads with 50 sequential<br>
 * With client authentication On a bi-core Centrino2 vPro: 3/s in 50 sequential, 27/s in 10 threads with 50 sequential
 *
 */
public class LocalExecSslClientTest extends Thread {

    static int nit = 30;
    static int nth = 1;
    static String command = "J:\\GG\\testexec.bat";
    static int port = 9999;
    static InetSocketAddress address;
    // with client authentication
    static String keyStoreFilename = "J:\\GG\\R66\\certs\\testclient2.jks";
    // without client authentication
    // static String keyStoreFilename = null;
    static String keyStorePasswd = "testclient2";
    static String keyPasswd = "client2";
    static String keyTrustStoreFilename = "J:\\GG\\R66\\certs\\testclient.jks";
    static String keyTrustStorePasswd = "testclient";
    static LocalExecResult result;

    static int ok = 0;
    static int ko = 0;
    static AtomicInteger atomicInteger = new AtomicInteger();

    static ExecutorService threadPool;
    static ExecutorService threadPool2;
    // Configure the client.
    static ClientBootstrap bootstrap;
    // Configure the pipeline factory.
    static LocalExecSslClientPipelineFactory localExecClientPipelineFactory;

    /**
     * Test & example main
     * @param args ignored
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(
                Level.WARN));
        InetAddress addr;
        byte []loop = {127,0,0,1};
        try {
            addr = InetAddress.getByAddress(loop);
        } catch (UnknownHostException e) {
            return;
        }
        address = new InetSocketAddress(addr, port);
        threadPool = Executors.newCachedThreadPool();
        threadPool2 = Executors.newCachedThreadPool();
        // Configure the client.
        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(threadPool, threadPool2));
        // Configure the pipeline factory.
        // First create the SSL part
        WaarpSecureKeyStore WaarpSecureKeyStore;
        // For empty KeyStore
        if (keyStoreFilename == null) {
            WaarpSecureKeyStore =
                new WaarpSecureKeyStore(keyStorePasswd, keyPasswd);
        } else {
            WaarpSecureKeyStore =
                new WaarpSecureKeyStore(keyStoreFilename, keyStorePasswd, keyPasswd);
        }

        if (keyTrustStoreFilename != null) {
            // Load the client TrustStore
            WaarpSecureKeyStore.initTrustStore(keyTrustStoreFilename, keyTrustStorePasswd, false);
        } else {
            WaarpSecureKeyStore.initEmptyTrustStore();
        }
        WaarpSslContextFactory waarpSslContextFactory = new WaarpSslContextFactory(WaarpSecureKeyStore, false);
        localExecClientPipelineFactory =
                new LocalExecSslClientPipelineFactory(waarpSslContextFactory);
        bootstrap.setPipelineFactory(localExecClientPipelineFactory);
        try {
            // Parse options.
            LocalExecSslClientTest client = new LocalExecSslClientTest();
            // run once
            long first = System.currentTimeMillis();
            client.connect();
            client.runOnce();
            client.disconnect();
            long second = System.currentTimeMillis();
            // print time for one exec
            System.err.println("1=Total time in ms: "+(second-first)+" or "+(1*1000/(second-first))+" exec/s");
            System.err.println("Result: " + ok+":"+ko);
            ok = 0;
            ko = 0;

            // Now run multiple within one thread
            first = System.currentTimeMillis();
            for (int i = 0; i < nit; i ++) {
                client.connect();
                client.runOnce();
                client.disconnect();
            }
            second = System.currentTimeMillis();
            // print time for one exec
            System.err.println(nit+"=Total time in ms: "+(second-first)+" or "+(nit*1000/(second-first))+" exec/s");
            System.err.println("Result: " + ok+":"+ko);
            ok = 0;
            ko = 0;

            // Now run multiple within multiple threads
            // Create multiple threads
            ExecutorService executorService = Executors.newFixedThreadPool(nth);
            first = System.currentTimeMillis();
            // Starts all thread with a default number of execution
            for (int i = 0; i < nth; i ++) {
                executorService.submit(new LocalExecSslClientTest());
            }
            Thread.sleep(500);
            executorService.shutdown();
            while (! executorService.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                Thread.sleep(50);
            }
            second = System.currentTimeMillis();

            // print time for one exec
            System.err.println((nit*nth)+"=Total time in ms: "+(second-first)+" or "+(nit*nth*1000/(second-first))+" exec/s");
            System.err.println("Result: " + ok+":"+ko);
            ok = 0;
            ko = 0;

            // run once
            first = System.currentTimeMillis();
            client.connect();
            client.runFinal();
            client.disconnect();
            second = System.currentTimeMillis();
            // print time for one exec
            System.err.println("1=Total time in ms: "+(second-first)+" or "+(1*1000/(second-first))+" exec/s");
            System.err.println("Result: " + ok+":"+ko);
            ok = 0;
            ko = 0;
        } finally {
            // Shut down all thread pools to exit.
            bootstrap.releaseExternalResources();
            localExecClientPipelineFactory.releaseResources();
        }
    }

    /**
     * Simple constructor
     */
    public LocalExecSslClientTest() {
    }

    private Channel channel;
    /**
     * Run method for thread
     */
    public void run() {
        connect();
        for (int i = 0; i < nit; i ++) {
            this.runOnce();
        }
        disconnect();
    }

    /**
     * Connect to the Server
     */
    private void connect() {
        // Start the connection attempt.
        ChannelFuture future = bootstrap.connect(address);

        // Wait until the connection attempt succeeds or fails.
        try {
            channel = future.await().getChannel();
        } catch (InterruptedException e) {
        }
        if (!future.isSuccess()) {
            System.err.println("Client Not Connected");
            future.getCause().printStackTrace();
            return;
        }
    }
    /**
     * Disconnect from the server
     */
    private void disconnect() {
    	SslHandler handler = (SslHandler) channel.getPipeline().get("ssl");
    	Thread.yield();
    	// Close the ssl and the connection.
		handler.close().addListener(new ChannelFutureListener() {
	        public void operationComplete(ChannelFuture future) {
	        	try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
	            future.getChannel().close();
	        }});
    }
    /**
     * Run method both for not threaded execution and threaded execution
     */
    private void runOnce() {
        // Initialize the command context
        LocalExecSslClientHandler clientHandler =
            (LocalExecSslClientHandler) channel.getPipeline().getLast();
        // Command to execute
        String line = command+" "+atomicInteger.incrementAndGet();
        clientHandler.initExecClient(0, line);
        // Wait for the end of the exec command
        LocalExecResult localExecResult = clientHandler.waitFor(10000);
        int status = localExecResult.status;
        if (status < 0) {
            System.err.println("Status: " + status + "\nResult: " +
                    localExecResult.result);
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
        LocalExecSslClientHandler clientHandler =
            (LocalExecSslClientHandler) channel.getPipeline().getLast();
        // Command to execute
        clientHandler.initExecClient(-1000, "stop");
        // Wait for the end of the exec command
        LocalExecResult localExecResult = clientHandler.waitFor(10000);
        int status = localExecResult.status;
        if (status < 0) {
            System.err.println("Status: " + status + "\nResult: " +
                    localExecResult.result);
            ko++;
        } else {
            ok++;
            result = localExecResult;
        }
    }
}
