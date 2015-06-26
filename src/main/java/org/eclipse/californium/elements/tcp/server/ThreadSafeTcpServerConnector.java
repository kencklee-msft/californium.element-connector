package org.eclipse.californium.elements.tcp.server;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.elements.config.TCPConnectionConfig;

public class ThreadSafeTcpServerConnector extends TcpServerConnector{
	private static final Logger LOG = Logger.getLogger( ThreadSafeTcpServerConnector.class.getName() );

	public final ReentrantLock lock = new ReentrantLock();
	public boolean isStarted = false;

	public ThreadSafeTcpServerConnector(final TCPConnectionConfig cfg) {
		super(cfg);
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
				LOG.log(Level.WARNING, "Connector already started");
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
				LOG.log(Level.WARNING, "Connector already stoped");
			}
		}
		finally {
			lock.unlock();
		}
	}

}
