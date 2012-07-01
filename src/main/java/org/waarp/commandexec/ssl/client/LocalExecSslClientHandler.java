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
package org.waarp.commandexec.ssl.client;

import javax.net.ssl.SSLException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.ssl.SslHandler;
import org.waarp.commandexec.client.LocalExecClientHandler;
import org.waarp.commandexec.client.LocalExecClientPipelineFactory;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.common.logging.WaarpInternalLogger;
import org.waarp.common.logging.WaarpInternalLoggerFactory;


/**
 * @author Frederic Bregier
 *
 */
public class LocalExecSslClientHandler extends LocalExecClientHandler {
    /**
     * Internal Logger
     */
    private static final WaarpInternalLogger logger = WaarpInternalLoggerFactory
            .getLogger(LocalExecSslClientHandler.class);
    /**
     * @param factory
     */
    public LocalExecSslClientHandler(LocalExecClientPipelineFactory factory) {
        super(factory);
    }
    /* (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
    	channel = ctx.getChannel();
        SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
        // Begin handshake
        ChannelFuture handshakeFuture = sslHandler.handshake();
        handshakeFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future)
                    throws Exception {
                logger.debug("Handshake: "+future.isSuccess(),future.getCause());
                if (future.isSuccess()) {
                    factory.addChannel(future.getChannel());
                } else {
                    future.getChannel().close();
                }
            }
        });
    }
    
    @Override
	public void actionBeforeClose(Channel channel) {
    	SslHandler handler = (SslHandler) channel.getPipeline().get("ssl");
    	try {
			handler.close().await();
		} catch (InterruptedException e1) {
		}
	}
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.warn("Unexpected exception from downstream while get information: "+firstMessage,
                e.getCause());
        if (firstMessage) {
            firstMessage = false;
            result.set(LocalExecDefaultResult.BadTransmition);
            result.exception = (Exception) e.getCause();
            back = new StringBuilder("Error in LocalExec: ");
            back.append(result.exception.getMessage());
            back.append('\n');
        } else {
        	if (e.getCause() instanceof SSLException) {
        		// ignore ?
        		logger.warn("Ignore exception ?", e.getCause());
        		return;
        	}
            back.append("\nERROR while receiving answer: ");
            result.exception = (Exception) e.getCause();
            back.append(result.exception.getMessage());
            back.append('\n');
        }
        actionBeforeClose(e.getChannel());
        e.getChannel().close();
    }
}
