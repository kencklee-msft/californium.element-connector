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

	private final AtomicBoolean isForwarded = new AtomicBoolean(false);
	private final AtomicBoolean isCancelled = new AtomicBoolean(false);
	
	private final ReentrantLock waitForForwardLock = new ReentrantLock();
	private final Condition doneOrCancelled = waitForForwardLock.newCondition();

	private Future<V> forwardFuture;

	public boolean setTransitiveFuture(final Future<V> future) {
		if(!isForwarded.getAndSet(true)) {
			waitForForwardLock.lock();
			try {
				forwardFuture = future;
				doneOrCancelled.signalAll();
			}
			finally {
				waitForForwardLock.unlock();
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
		if(isForwarded.get() && !isCancelled.get()) {
			return forwardFuture.cancel(mayInterruptIfRunning);
		}
		else {
			final boolean cancelled =  isCancelled.compareAndSet(false, true);
			waitForForwardLock.lock(); 
			try {
				doneOrCancelled.signalAll();
			}
			finally{
				waitForForwardLock.unlock();
			}
			return cancelled;
		}
	}

	@Override
	public boolean isCancelled() {
		if(!isCancelled.get() || isForwarded.get()) {
			return forwardFuture.isCancelled();
		}
		return isCancelled.get();
	}

	@Override
	public boolean isDone() {
		if(isCancelled.get()) {
			return true;
		}
		if(isForwarded.get()) {
			return forwardFuture.isDone();
		}
		return false;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		if(!isCancelled.get()) {
			waitForFoward();
			return forwardFuture.get();
		}
		else {
			throw new CancellationException("Future was cancelled");
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
				return forwardFuture.get(remaider, TimeUnit.MILLISECONDS);
			}
		}
		throw new CancellationException("Future was cancelled");
	}
	
	private void waitForFoward() throws InterruptedException {
		if(!isForwarded.get()){
			waitForForwardLock.lock();
			try {
				doneOrCancelled.await();
			}
			finally {
				waitForForwardLock.unlock();
			}
		}
	}
	
	private boolean waitForFoward(final long timeout, final TimeUnit unit) throws InterruptedException {
		if(!isForwarded.get()){
			waitForForwardLock.lock();
			try {
				return doneOrCancelled.await(timeout, unit);
			}
			finally {
				waitForForwardLock.unlock();
			}
		}
		return true;
	}

}
