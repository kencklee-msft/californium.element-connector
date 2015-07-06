package org.eclipse.californium.elements.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FutureAggregate<V> implements Future<V>{
	
	private final Future<V>[] futures;
	private final AtomicBoolean aggregateCanceled = new AtomicBoolean(false);
	private final AtomicBoolean aggregateDone = new AtomicBoolean(false);
	
	public FutureAggregate(final Future<V>... futures) {
		this.futures = futures;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		for(final Future<V> f : futures) {
			if(aggregateCanceled.get()) {
				aggregateCanceled.set(f.cancel(mayInterruptIfRunning));
			}
		}
		return aggregateCanceled.get();
	}

	@Override
	public boolean isCancelled() {
		return aggregateCanceled.get();
	}

	@Override
	public boolean isDone() {
		return aggregateDone.get();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		for(final Future<V> f: futures) {
			f.get();
		}
		return null;
	}

	@Override
	public V get(final long timeout, final TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		if(futures.length > 0) {
			long timeoutMs = unit.toMillis(timeout);
			for(final Future<V> f : futures) {
				if(timeoutMs > 0) {
				final long before = System.currentTimeMillis();
				f.get(timeoutMs, TimeUnit.MILLISECONDS);
				final long delta = System.currentTimeMillis() - before;
				timeoutMs = timeoutMs - delta;
				} else {
					throw new TimeoutException("Could not finish all action withing given time");
				}
			}
		}
		return null;
	}

	
}
