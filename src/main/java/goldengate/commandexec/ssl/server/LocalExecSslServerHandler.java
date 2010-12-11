/**
   This file is part of GoldenGate Project (named also GoldenGate or GG).

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All GoldenGate Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   GoldenGate is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GoldenGate .  If not, see <http://www.gnu.org/licenses/>.
 */
package goldengate.commandexec.ssl.server;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.ssl.SslHandler;

import goldengate.commandexec.server.LocalExecServerHandler;
import goldengate.commandexec.server.LocalExecServerPipelineFactory;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

/**
 * @author Frederic Bregier
 *
 */
public class LocalExecSslServerHandler extends LocalExecServerHandler {
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(LocalExecSslServerHandler.class);

    /**
     * @param factory
     * @param newdelay
     */
    public LocalExecSslServerHandler(LocalExecServerPipelineFactory factory,
            long newdelay) {
        super(factory, newdelay);
    }
    /* (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
        // Begin handshake
        ChannelFuture handshakeFuture = sslHandler.handshake();
        handshakeFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future)
                    throws Exception {
                logger.debug("Handshake: "+future.isSuccess(),future.getCause());
                if (future.isSuccess()) {
                    if (isShutdown(future.getChannel())) {
                        answered = true;
                        return;
                    }
                    answered = false;
                    factory.addChannel(future.getChannel());
                } else {
                    answered = true;
                    future.getChannel().close();
                }
            }
        });
    }
}
