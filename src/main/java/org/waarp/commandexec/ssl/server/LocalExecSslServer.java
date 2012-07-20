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
package org.waarp.commandexec.ssl.server;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;

/**
 * LocalExec server Main method.
 *
 */
public class LocalExecSslServer {

    static ExecutorService threadPool;
    static ExecutorService threadPool2;

    /**
     * Takes 3 to 8 arguments (last 5 are optional arguments):<br>
     * - mandatory arguments: filename keystorepaswwd keypassword<br>
     * - if no more arguments are provided, it implies 127.0.0.1 + 9999 as port and no certificates<br>
     * - optional arguments:<br>
     *  "port"<br>
     *  "port" "trustfilename" "trustpassword"<br>
     *  "port" "trustfilename" "trustpassword" "addresse"<br>
     *  "port" "trustfilename" "trustpassword" "addresse" "default delay"<br>
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        int port = 9999;
        InetAddress addr;
        long delay = LocalExecDefaultResult.MAXWAITPROCESS;
        String keyStoreFilename, keyStorePasswd, keyPassword;
        String trustStoreFilename = null, trustStorePasswd = null;
        byte []loop = {127,0,0,1};
        addr = InetAddress.getByAddress(loop);
        if (args.length >=3) {
            keyStoreFilename = args[0];
            keyStorePasswd = args[1];
            keyPassword = args[2];
            if (args.length >= 4) {
                port = Integer.parseInt(args[3]);
                if (args.length >= 6) {
	                trustStoreFilename = args[4];
	                trustStorePasswd = args[5];
	                if (args.length >= 7) {
	                    addr = InetAddress.getByName(args[6]);
	                    if (args.length > 7) {
	                        delay = Long.parseLong(args[7]);
	                    }
	                }
                }
            }
        } else {
            System.err.println("Need at least 3 arguments: Filename KeyStorePswd KeyPswd");
            return;
        }
        threadPool = Executors.newCachedThreadPool();
        threadPool2 = Executors.newCachedThreadPool();
        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(threadPool, threadPool2));
        // Load the KeyStore (No certificates)
        WaarpSecureKeyStore WaarpSecureKeyStore =
            new WaarpSecureKeyStore(keyStoreFilename, keyStorePasswd, keyPassword);
        if (trustStoreFilename != null) {
            // Include certificates
            WaarpSecureKeyStore.initTrustStore(trustStoreFilename, trustStorePasswd, true);
        } else {
            WaarpSecureKeyStore.initEmptyTrustStore();
        }
        WaarpSslContextFactory waarpSslContextFactory =
            new WaarpSslContextFactory(WaarpSecureKeyStore, true);
        // Configure the pipeline factory.
        bootstrap.setPipelineFactory(
                new LocalExecSslServerPipelineFactory(waarpSslContextFactory, delay));

        // Bind and start to accept incoming connections only on local address.
        bootstrap.bind(new InetSocketAddress(addr, port));
    }
}
