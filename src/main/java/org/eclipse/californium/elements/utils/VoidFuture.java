package org.eclipse.californium.elements.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VoidFuture implements Future<Void>{
	
	public static final Void voidType = null;

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public Void get(final long timeout, final TimeUnit unit) 
			throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

}
