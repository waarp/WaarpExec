/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3.0 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package goldengate.commandexec.server;

import goldengate.commandexec.utils.LocalExecDefaultResult;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Handles a server-side channel for LocalExec.
 *
 *
 */
public class LocalExecServerHandler extends SimpleChannelUpstreamHandler {
    // Fixed delay, but could change if necessary at construction
    private long delay = LocalExecDefaultResult.MAXWAITPROCESS;
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(LocalExecServerHandler.class);

    /**
     * Constructor with a specific delay
     * @param newdelay
     */
    public LocalExecServerHandler(long newdelay) {
        delay = newdelay;
    }

    /**
     * Change the delay to the specific value. Need to be called before any receive message.
     * @param newdelay
     */
    public void setNewDelay(long newdelay) {
        delay = newdelay;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent evt) {
        // Cast to a String first.
        // We know it is a String because we put some codec in
        // LocalExecPipelineFactory.
        String request = (String) evt.getMessage();

        // Generate and write a response.
        String response;
        response = LocalExecDefaultResult.NoStatus.status+" "+
            LocalExecDefaultResult.NoStatus.result;
        ExecuteWatchdog watchdog = null;
        try {
            if (request.length() == 0) {
                // No command
                response = LocalExecDefaultResult.NoCommand.status+" "+
                    LocalExecDefaultResult.NoCommand.result;
            } else {
                String[] args = request.split(" ");
                File exec = new File(args[0]);
                if (exec.isAbsolute()) {
                    // If true file, is it executable
                    if (! exec.canExecute()) {
                        logger.error("Exec command is not executable: " + request);
                        response = LocalExecDefaultResult.NotExecutable.status+" "+
                            LocalExecDefaultResult.NotExecutable.result;
                        return;
                    }
                }
                // Create command with parameters
                CommandLine commandLine = new CommandLine(args[0]);
                for (int i = 1; i < args.length; i ++) {
                    commandLine.addArgument(args[i]);
                }
                DefaultExecutor defaultExecutor = new DefaultExecutor();
                ByteArrayOutputStream outputStream;
                outputStream = new ByteArrayOutputStream();
                PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(outputStream);
                defaultExecutor.setStreamHandler(pumpStreamHandler);
                int[] correctValues = { 0, 1 };
                defaultExecutor.setExitValues(correctValues);
                if (delay > 0) {
                    // If delay (max time), then setup Watchdog
                    watchdog = new ExecuteWatchdog(delay);
                    defaultExecutor.setWatchdog(watchdog);
                }
                int status = -1;
                try {
                    // Execute the command
                    status = defaultExecutor.execute(commandLine);
                } catch (ExecuteException e) {
                    if (e.getExitValue() == -559038737) {
                        // Cannot run immediately so retry once
                        try {
                            Thread.sleep(LocalExecDefaultResult.RETRYINMS);
                        } catch (InterruptedException e1) {
                        }
                        try {
                            status = defaultExecutor.execute(commandLine);
                        } catch (ExecuteException e1) {
                            pumpStreamHandler.stop();
                            logger.error("Exception: " + e.getMessage() +
                                    " Exec in error with " + commandLine.toString());
                            response = LocalExecDefaultResult.BadExecution.status+" "+
                                LocalExecDefaultResult.BadExecution.result;
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                            }
                            return;
                        } catch (IOException e1) {
                            pumpStreamHandler.stop();
                            logger.error("Exception: " + e.getMessage() +
                                    " Exec in error with " + commandLine.toString());
                            response = LocalExecDefaultResult.BadExecution.status+" "+
                                LocalExecDefaultResult.BadExecution.result;
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                            }
                            return;
                        }
                    } else {
                        pumpStreamHandler.stop();
                        logger.error("Exception: " + e.getMessage() +
                                " Exec in error with " + commandLine.toString());
                        response = LocalExecDefaultResult.BadExecution.status+" "+
                            LocalExecDefaultResult.BadExecution.result;
                        try {
                            outputStream.close();
                        } catch (IOException e2) {
                        }
                        return;
                    }
                } catch (IOException e) {
                    pumpStreamHandler.stop();
                    logger.error("Exception: " + e.getMessage() +
                            " Exec in error with " + commandLine.toString());
                    response = LocalExecDefaultResult.BadExecution.status+" "+
                        LocalExecDefaultResult.BadExecution.result;
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                    return;
                }
                pumpStreamHandler.stop();
                if (defaultExecutor.isFailure(status) && watchdog != null &&
                        watchdog.killedProcess()) {
                    // kill by the watchdoc (time out)
                    logger.error("Exec is in Time Out");
                    response = LocalExecDefaultResult.TimeOutExecution.status+" "+
                        LocalExecDefaultResult.TimeOutExecution.result;
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                } else {
                    response = status+" "+outputStream.toString()+"\n";
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                }
            }
        } finally {
            // We do not need to write a ChannelBuffer here.
            // We know the encoder inserted at LocalExecPipelineFactory will do the
            // conversion.
            ChannelFuture future = evt.getChannel().write(response);
            if (watchdog != null) {
                watchdog.stop();
            }
            // Close connection after each execution
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.error("Unexpected exception from downstream.", e
                .getCause());
        // Nothing to do since execution will stop later on and an error will occur on client side
        // since no message arrived before close (or partially)
        e.getChannel().close();
    }
}
