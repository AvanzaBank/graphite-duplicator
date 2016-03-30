/*
 * Copyright 2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.graphite.duplicator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * @author Kristoffer Erlandsson
 */
public class UdpReceiver {

	private static final int RECEIVE_BUFFER_SIZE = 16777216;
	Logger log = new Logger(UdpReceiver.class);
	
	private final int port;
	private DatagramSocket serverSocket;
	private byte[] receiveData = new byte[512];
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	private Consumer<String> packetConsumer;
	
	private Thread t = new Thread(() -> {
		while (!Thread.interrupted()) {
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			String s = new String(receiveData, receivePacket.getOffset(), receivePacket.getLength());
			s = s.trim();
			packetConsumer.accept(s);
		}
	});

	public UdpReceiver(int port, Consumer<String> packetConsumer) {
		t.setName("udp-receiver");
		this.packetConsumer = packetConsumer;
		try {
			serverSocket = new DatagramSocket(port);
			serverSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
			this.port = serverSocket.getLocalPort();
			log.info("Listening on port " + port + ", receive buffer size " + serverSocket.getReceiveBufferSize());
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	public void start() {
		t.start();
	}
	
	public void stop() {
		t.interrupt();
		try {
			t.join();
		} catch (InterruptedException e) {
		}
	}

	public int getPort() {
		return port;
	}

}
