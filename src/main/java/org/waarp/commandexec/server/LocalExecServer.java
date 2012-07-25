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
package org.waarp.commandexec.server;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpThreadFactory;

/**
 * LocalExec server Main method.
 *
 *
 */
public class LocalExecServer {

    static ExecutorService threadPool;
    static ExecutorService threadPool2;
    static OrderedMemoryAwareThreadPoolExecutor pipelineExecutor = 
    		new OrderedMemoryAwareThreadPoolExecutor(
			1000, 0, 0, 200, TimeUnit.MILLISECONDS,
			new WaarpThreadFactory("CommandExecutor"));

    /**
     * Takes 3 optional arguments:<br>
     * - no argument: implies 127.0.0.1 + 9999 port<br>
     * - arguments:<br>
     *  "addresse" "port"<br>
     *  "addresse" "port" "default delay"<br>
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        int port = 9999;
        InetAddress addr;
        long delay = LocalExecDefaultResult.MAXWAITPROCESS;
        if (args.length >=2) {
            addr = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);
            if (args.length > 2) {
                delay = Long.parseLong(args[2]);
            }
        } else {
            byte []loop = {127,0,0,1};
            addr = InetAddress.getByAddress(loop);
        }
        threadPool = Executors.newCachedThreadPool();
        threadPool2 = Executors.newCachedThreadPool();
        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(threadPool, threadPool2));

        // Configure the pipeline factory.
        bootstrap.setPipelineFactory(new LocalExecServerPipelineFactory(delay, pipelineExecutor));

        // Bind and start to accept incoming connections only on local address.
        bootstrap.bind(new InetSocketAddress(addr, port));
    }
}
