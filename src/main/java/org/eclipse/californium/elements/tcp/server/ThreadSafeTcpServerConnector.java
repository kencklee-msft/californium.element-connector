package org.eclipse.californium.elements.tcp.server;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.californium.elements.config.TCPConnectionConfig;

public class ThreadSafeTcpServerConnector extends TcpServerConnector{
	public final ReentrantLock lock = new ReentrantLock();
	public boolean isStarted = false;
	
	public ThreadSafeTcpServerConnector(final TCPConnectionConfig cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
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
