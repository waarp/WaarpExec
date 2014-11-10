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
package org.waarp.commandexec.client;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpInternalLogger;
import org.waarp.common.logging.WaarpInternalLoggerFactory;

/**
 * Handles a client-side channel for LocalExec
 * 
 * 
 */
public class LocalExecClientHandler extends SimpleChannelUpstreamHandler {

    /**
     * Internal Logger
     */
    private static final WaarpInternalLogger logger = WaarpInternalLoggerFactory
            .getLogger(LocalExecClientHandler.class);

    protected LocalExecResult result;
    protected StringBuilder back;
    protected boolean firstMessage = true;
    protected WaarpFuture future;
    protected LocalExecClientPipelineFactory factory = null;
    protected long delay;
    protected String command;
    protected Channel channel;
    protected WaarpFuture ready = new WaarpFuture(true);

    /**
     * Constructor
     */
    public LocalExecClientHandler(LocalExecClientPipelineFactory factory) {
        this.factory = factory;
    }

    /**
     * Initialize the client status for a new execution
     * 
     * @param delay
     * @param command
     */
    public void initExecClient(long delay, String command) {
        this.result = new LocalExecResult(LocalExecDefaultResult.NoStatus);
        this.back = new StringBuilder();
        this.firstMessage = true;
        this.future = new WaarpFuture(true);
        this.delay = delay;
        this.command = command;
        // Sends the received line to the server.
        if (channel == null) {
            try {
                ready.await();
            } catch (InterruptedException e) {}
        }
        logger.debug("write command: " + this.command);
        try {
            if (this.delay != 0) {
                channel.write(this.delay + " " + this.command + "\n").await();
            } else {
                channel.write(this.command + "\n").await();
            }
        } catch (InterruptedException e) {}
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        channel = ctx.getChannel();
        factory.addChannel(channel);
        ready.setSuccess();
    }

    /**
     * When closed, <br>
     * If no messaged were received => NoMessage error is set to future<br>
     * Else if an error was detected => Set the future to error (with or without exception)<br>
     * Else if no error occurs => Set success to the future<br>
     * 
     * 
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        if (future == null || !future.isDone()) {
            // Should not be
            finalizeMessage();
        }
        super.channelClosed(ctx, e);
    }

    /**
     * Finalize a message
     */
    private void finalizeMessage() {
        if (firstMessage) {
            result.set(LocalExecDefaultResult.NoMessage);
        } else {
            result.result = back.toString();
        }
        if (result.status < 0) {
            if (result.exception != null) {
                this.future.setFailure(result.exception);
            } else {
                this.future.cancel();
            }
        } else {
            this.future.setSuccess();
        }
    }

    /**
     * Waiting for the close of the exec
     * 
     * @return The LocalExecResult
     */
    public LocalExecResult waitFor(long delay) {
        if (delay <= 0) {
            this.future.awaitUninterruptibly();
        } else {
            this.future.awaitUninterruptibly(delay);
        }
        result.isSuccess = this.future.isSuccess();
        return result;
    }

    /**
     * Action to do before close
     */
    public void actionBeforeClose(Channel channel) {
        // here nothing to do
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        // Add the line received from the server.
        String mesg = (String) e.getMessage();
        // If first message, then take the status and then the message
        if (firstMessage) {
            firstMessage = false;
            int pos = mesg.indexOf(' ');
            try {
                result.status = Integer.parseInt(mesg.substring(0, pos));
            } catch (NumberFormatException e1) {
                // Error
                logger.debug(this.command + ":" + "Bad Transmission: " + mesg + "\n\t" + back.toString());
                result.set(LocalExecDefaultResult.BadTransmition);
                back.append(mesg);
                actionBeforeClose(e.getChannel());
                WaarpSslUtility.closingSslChannel(ctx.getChannel());
                return;
            }
            mesg = mesg.substring(pos + 1);
            if (mesg.startsWith(LocalExecDefaultResult.ENDOFCOMMAND)) {
                logger.debug(this.command + ":" + "Receive End of Command");
                result.result = LocalExecDefaultResult.NoMessage.result;
                back.append(result.result);
                this.finalizeMessage();
            } else {
                result.result = mesg;
                back.append(mesg);
            }
        } else if (mesg.startsWith(LocalExecDefaultResult.ENDOFCOMMAND)) {
            logger.debug(this.command + ":" + "Receive End of Command");
            this.finalizeMessage();
        } else {
            back.append('\n');
            back.append(mesg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.warn(this.command + ":" + "Unexpected exception from downstream while get information: " + firstMessage,
                e.getCause());
        if (firstMessage) {
            firstMessage = false;
            result.set(LocalExecDefaultResult.BadTransmition);
            result.exception = (Exception) e.getCause();
            back = new StringBuilder("Error in LocalExec: ");
            back.append(result.exception.getMessage());
            back.append('\n');
        } else {
            back.append("\nERROR while receiving answer: ");
            result.exception = (Exception) e.getCause();
            back.append(result.exception.getMessage());
            back.append('\n');
        }
        actionBeforeClose(e.getChannel());
        WaarpSslUtility.closingSslChannel(e.getChannel());
    }
}
