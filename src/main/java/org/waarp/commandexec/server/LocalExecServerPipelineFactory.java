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

import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.waarp.commandexec.utils.LocalExecDefaultResult;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel for LocalExecServer.
 *
 *
 */
public class LocalExecServerPipelineFactory implements ChannelPipelineFactory {

    private long delay = LocalExecDefaultResult.MAXWAITPROCESS;
    private final ChannelGroup channelGroup = new DefaultChannelGroup("LocalExecServer");
    protected final OrderedMemoryAwareThreadPoolExecutor omatpe;

    /**
     * Constructor with default delay
     * 
     */
    public LocalExecServerPipelineFactory(OrderedMemoryAwareThreadPoolExecutor omatpe) {
        // Default delay
        this.omatpe = omatpe;
    }

    /**
     * Constructor with a specific default delay
     * @param newdelay
     */
    public LocalExecServerPipelineFactory(long newdelay,
    		OrderedMemoryAwareThreadPoolExecutor omatpe) {
        delay = newdelay;
        this.omatpe = omatpe;
    }


    public ChannelPipeline getPipeline() throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();

        // Add the text line codec combination first,
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
                Delimiters.lineDelimiter()));
        pipeline.addLast("pipelineExecutor", new ExecutionHandler(omatpe));
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());

        // and then business logic.
        // Could change it with a new fixed delay if necessary at construction
        pipeline.addLast("handler", new LocalExecServerHandler(this, delay));

        return pipeline;
    }
    /**
     * Add a channel to the ExecClient Group
     * @param channel
     */
    public void addChannel(Channel channel) {
        channelGroup.add(channel);
    }

    /**
     * Release internal resources
     */
    public void releaseResources() {
        channelGroup.close();
    }
}
