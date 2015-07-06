package org.eclipse.californium.elements.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is meant to be an empty shell for an upcoming Future.  If an operation is implicit but
 * happens on a different channel, this can be used as a place holder
 *
 * @param <V>
 */
public final class TransitiveFuture<V> implements Future<V> {

	private final AtomicBoolean isFowarded = new AtomicBoolean(false);
	private final AtomicBoolean isCancelled = new AtomicBoolean(false);
	
	private final ReentrantLock waitForFowardLock = new ReentrantLock();
	private final Condition doneOrCanclled = waitForFowardLock.newCondition();

	private Future<V> fowardFuture;

	public boolean setTransitiveFuture(final Future<V> future) {
		if(!isFowarded.getAndSet(true)) {
			waitForFowardLock.lock();
			try {
				fowardFuture = future;
				doneOrCanclled.signalAll();
			}
			finally {
				waitForFowardLock.unlock();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		if(isCancelled.get()) {
			return false;
		}
		if(isFowarded.get() && !isCancelled.get()) {
			return fowardFuture.cancel(mayInterruptIfRunning);
		}
		else {
			final boolean canceled =  isCancelled.compareAndSet(false, true);
			waitForFowardLock.lock(); 
			try {
				doneOrCanclled.signalAll();
			}
			finally{
				waitForFowardLock.unlock();
			}
			return canceled;
		}
	}

	@Override
	public boolean isCancelled() {
		if(!isCancelled.get() || isFowarded.get()) {
			return fowardFuture.isCancelled();
		}
		return isCancelled.get();
	}

	@Override
	public boolean isDone() {
		if(isCancelled.get()) {
			return true;
		}
		if(isFowarded.get()) {
			return fowardFuture.isDone();
		}
		return false;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		if(!isCancelled.get()) {
			waitForFoward();
			return fowardFuture.get();
		}
		else {
			throw new CancellationException("Future was Canceled");
		}		
	}

	@Override
	public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if(!isCancelled.get()) {
			final long timeoutMs = unit.toMillis(timeout);
			final long before = System.currentTimeMillis();
			final boolean reachTimeout = waitForFoward(timeout, unit);
			if(!reachTimeout) {
				final long delta = System.currentTimeMillis() - before;
				final long remaider = timeoutMs - delta;
				return fowardFuture.get(remaider, TimeUnit.MILLISECONDS);
			}
		}
		throw new CancellationException("Future was Canceled");
	}
	
	private void waitForFoward() throws InterruptedException {
		if(!isFowarded.get()){
			waitForFowardLock.lock();
			try {
				doneOrCanclled.await();
			}
			finally {
				waitForFowardLock.unlock();
			}
		}
	}
	
	private boolean waitForFoward(final long timeout, final TimeUnit unit) throws InterruptedException {
		if(!isFowarded.get()){
			waitForFowardLock.lock();
			try {
				return doneOrCanclled.await(timeout, unit);
			}
			finally {
				waitForFowardLock.unlock();
			}
		}
		return true;
	}

}
