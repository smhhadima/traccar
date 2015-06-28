package org.traccar.protocol;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.protocol.commands.CommandTemplate;
import org.traccar.http.commands.CommandType;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

public class CastelProtocol extends BaseProtocol {

    public CastelProtocol() {
        super("castel");
    }

    @Override
    protected void loadCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
        
    }

    @Override
    public void addTrackerServersTo(List<TrackerServer> serverList) {
        TrackerServer server = new TrackerServer(new ServerBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, -4, 0));
                pipeline.addLast("objectDecoder", new CastelProtocolDecoder(CastelProtocol.this));
            }
        };
        server.setEndianness(ByteOrder.LITTLE_ENDIAN);
        serverList.add(server);

        server = new TrackerServer(new ConnectionlessBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("objectDecoder", new CastelProtocolDecoder(CastelProtocol.this));
            }
        };
        server.setEndianness(ByteOrder.LITTLE_ENDIAN);
        serverList.add(server);
    }
}
