/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class FifotrackProtocolDecoder extends BaseProtocolDecoder {

    public FifotrackProtocolDecoder(FifotrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("d+,")                       // index
            .expression("[^,]+,")                // type
            .number("(d+)?,")                    // alarm
            .number("(dd)(dd)(dd)")              // date
            .number("(dd)(dd)(dd),")             // time
            .number("([AV]),")                   // validity
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // odometer
            .number("d+,")                       // runtime
            .number("(xxxx),")                   // status
            .number("(x+),")                     // input
            .number("(x+),")                     // output
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)|")                     // lac
            .number("(x+),")                     // cid
            .number("([x|]+),")                  // adc
            .expression("([^,]+),")              // rfid
            .expression("([^*]+)")               // sensors
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        parser.next(); // alarm

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());

        position.set(Position.KEY_ODOMETER, parser.nextLong());
        position.set(Position.KEY_STATUS, parser.nextInt(16));
        position.set(Position.KEY_INPUT, parser.nextInt(16));
        position.set(Position.KEY_OUTPUT, parser.nextInt(16));

        position.set(Position.KEY_MCC, parser.nextInt());
        position.set(Position.KEY_MNC, parser.nextInt());
        position.set(Position.KEY_LAC, parser.nextInt(16));
        position.set(Position.KEY_CID, parser.nextInt(16));

        String[] adc = parser.next().split("\\|");
        for (int i = 0; i < adc.length; i++) {
            position.set(Position.PREFIX_ADC + (i + 1), Integer.parseInt(adc[i], 16));
        }

        position.set(Position.KEY_RFID, parser.next());

        String[] sensors = parser.next().split("\\|");
        for (int i = 0; i < sensors.length; i++) {
            position.set(Position.PREFIX_IO + (i + 1), sensors[i]);
        }

        return position;
    }

}
