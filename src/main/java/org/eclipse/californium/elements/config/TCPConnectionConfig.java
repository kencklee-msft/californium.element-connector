package org.eclipse.californium.elements.config;

import io.netty.channel.ChannelOption;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public abstract class TCPConnectionConfig extends ConnectionConfig{

	private final CommunicationRole role;
	private final Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
	private boolean isSecure = false;
	private SSLContext sslContext;	
	private SSLCLientCertReq reqCertificate;
	private String[] tlsVersion;
	
	public enum SSLCLientCertReq {
		NONE,
		WANT,
		NEED
	}
	
	public TCPConnectionConfig(final CommunicationRole role) {
		this.role = role;
	}

	@Override
	public final LayerSemantic getTransportLayer() {
		return LayerSemantic.TCP;
	}
	
	@Override
	public final CommunicationRole getCommunicationRole() {
		return role;
	}
	
	/**
	 * only NIO is implemented for now
	 * @return
	 */
	public final ConnectionSemantic getConnectionSemantic() {
		return ConnectionSemantic.NIO;
	}
	
	/**
	 * set whether the connection is shared amongst all endpoint
	 * @return
	 */
	public boolean isSharable(){
		//default implementation
		return getCommunicationRole().equals(CommunicationRole.SERVER);
	}
	
	/**
	 * if no ConnectionState Listener 
	 * is supplied, event will not be sent up
	 * @return
	 */
	public ConnectionStateListener getListener() {
		//default implementation
		return null;
	}
	
	/**
	 * add any options the Channel
	 * @param option
	 * @param value
	 */
	public <T> void addChannelOption(final ChannelOption<T> option, final T value) {
		//default implementation
		options.put(option, value);
	}
	
	/**
	 * return the map of option
	 * @return
	 */
	public final Map<ChannelOption<?>, Object> getChannelOptions() {
		return options;
	}
	
	/**
	 * get the Executor containing the Threads used to send callback and notify back 
	 * to Californium
	 * @return
	 */
	public Executor getCallBackExecutor() {
		//default implementation
		return Executors.newCachedThreadPool();
	}
	
	/**
	 * set you SSL details for a socket server
	 * pass in the SSL context, the supported TLS version and if there is a need or obligation 
	 * for the client to have a certificate
	 * @param context
	 * @param reqCertificate
	 * @param tlsVersions
	 */
	public final void setServerSSL(final SSLContext context, final SSLCLientCertReq reqCertificate, final String... tlsVersions) {
		if(role.equals(CommunicationRole.SERVER)) {
			this.isSecure = true;
			this.sslContext = context;
			this.reqCertificate = reqCertificate;
			this.tlsVersion = tlsVersions;
		}
		else {
			throw new IllegalArgumentException("Cannot set Server side security on a TCP Client");
		}
	}
	
	/**
	 * set the SSL context for the TCP Client
	 * @param context
	 */
	public void setClientSSL(final SSLContext context) {
		if(role.equals(CommunicationRole.CLIENT)) {
			this.isSecure = true;
			this.sslContext = context;
		}
		else {
			throw new IllegalArgumentException("Cannot set Client side security on a TCP Server");
		}
	}
	
	public final boolean isSecured() {
		return isSecure;
	}
	
	public final SSLCLientCertReq getSslClientCertificateRequestLevel() {
		return reqCertificate;
	}
	
	public final String[] getTLSVersions() {
		return tlsVersion;
	}
	
	public final SSLContext getSSlContext() {
		return sslContext;
	}

}
