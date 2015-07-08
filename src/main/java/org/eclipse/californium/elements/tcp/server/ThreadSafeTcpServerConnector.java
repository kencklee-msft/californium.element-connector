package org.eclipse.californium.elements.tcp.server;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.elements.config.TCPConnectionConfig;

public class ThreadSafeTcpServerConnector extends TcpServerConnector{
	private static final Logger LOG = Logger.getLogger( ThreadSafeTcpServerConnector.class.getName() );

	public final ReentrantLock lock = new ReentrantLock();
	public boolean isStarted = false;
	public Future<?> startFuture;
	public Future<?> stopFuture;

	public ThreadSafeTcpServerConnector(final TCPConnectionConfig cfg) {
		super(cfg);
	}

	@Override
	public Future<?> start() throws IOException {
		lock.lock();
		try {
			if(!isStarted) {
				startFuture = super.start();
				isStarted = true;
			}
			else {
				LOG.log(Level.WARNING, "Connector already started");
			}
		}
		finally {
			lock.unlock();
		}
		return startFuture;
	}

	@Override
	public Future<?> stop() {
		lock.lock();
		try {
			if(isStarted) {
				stopFuture = super.stop();
				isStarted = false;
			}
			else {
				LOG.log(Level.WARNING, "Connector already stopped");
			}
		}
		finally {
			lock.unlock();
		}
		return stopFuture;
	}

}
