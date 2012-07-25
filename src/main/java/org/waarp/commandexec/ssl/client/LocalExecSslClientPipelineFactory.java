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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.waarp.commandexec.client.LocalExecClientPipelineFactory;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;


/**
 * Version with SSL support
 *
 *
 * @author Frederic Bregier
 *
 */
public class LocalExecSslClientPipelineFactory extends LocalExecClientPipelineFactory {

    private final WaarpSslContextFactory waarpSslContextFactory;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LocalExecSslClientPipelineFactory(WaarpSslContextFactory waarpSslContextFactory) {
        this.waarpSslContextFactory = waarpSslContextFactory;
    }

    public ChannelPipeline getPipeline() throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = Channels.pipeline();

        // Add SSL as first element in the pipeline
        SslHandler sslhandler = waarpSslContextFactory.initPipelineFactory(false,
                waarpSslContextFactory.needClientAuthentication(), false, executor);
        sslhandler.setIssueHandshake(true);
        pipeline.addLast("ssl", sslhandler);
        // Add the text line codec combination first,
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
                Delimiters.lineDelimiter()));
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());

        // and then business logic.
        LocalExecSslClientHandler localExecClientHandler = new LocalExecSslClientHandler(this);
        pipeline.addLast("handler", localExecClientHandler);

        return pipeline;
    }
    /**
     * Release internal resources
     */
    public void releaseResources() {
        super.releaseResources();
        this.executor.shutdownNow();
    }

}
