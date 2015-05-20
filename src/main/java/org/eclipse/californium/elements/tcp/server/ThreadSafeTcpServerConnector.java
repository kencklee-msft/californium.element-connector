package org.eclipse.californium.elements.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public class ThreadSafeTcpServerConnector extends TcpServerConnector{
	
	public final ReentrantLock lock = new ReentrantLock();
	public boolean isStarted = false;
	
	public ThreadSafeTcpServerConnector(final String address, final int port, final ConnectionStateListener csl) {
		super(new InetSocketAddress(address, port), csl);
	}

	public ThreadSafeTcpServerConnector(final InetSocketAddress address, final ConnectionStateListener csl) {
		super(address, csl);
	}
	
	
	@Override
	public void start(final boolean wait) throws IOException {
		lock.lock();
		try {
			if(!isStarted) {
				super.start(wait);
				isStarted = true;
			}
			else {
				System.out.println("Connector already started");
			}
		}
		finally {
			lock.unlock();
		}
	}
	
	@Override
	public void stop() {
		lock.lock();
		try {
			if(isStarted) {
				super.stop();
				isStarted = false;
			}
			else {
				System.out.println("Connector already stoped");
			}
		}
		finally {
			lock.unlock();
		}
	}

}
