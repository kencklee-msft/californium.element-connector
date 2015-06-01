package org.eclipse.californium.elements.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public class TCPConnectionConfig extends ConnectionConfig{

	private final CommunicationRole role;
	private boolean isSharable;
	private ConnectionSemantic connSem;
	private ConnectionStateListener listener;
	private final Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
	private Executor callbackExecutor;
	private boolean isSecure;
	private SslContext sslContext;
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
	public LayerSemantic getTransportLayer() {
		return LayerSemantic.TCP;
	}
	
	@Override
	public CommunicationRole getCommunicationRole() {
		return role;
	}
	
	public void setConnectionSemantics(final ConnectionSemantic connSem) {
		this.connSem = connSem;
	}
	
	public ConnectionSemantic getConnectionSemantic() {
		return connSem;
	}
	
	public void setSharable(final boolean isSharable) {
		this.isSharable = isSharable;
	}
	
	public boolean isSharable(){
		return isSharable;
	}
	
	public void setConnectionStateListener(final ConnectionStateListener listener) {
		this.listener = listener;
	}
	
	public ConnectionStateListener getListener() {
		return listener;
	}
	
	public <T> void addChannelOption(final ChannelOption<T> option, final T value) {
		options.put(option, value);
	}
	
	public void setCallBackExecutor(final Executor executor) {
		this.callbackExecutor = executor;
	}
	
	public Executor getCallBackExecutor() {
		return callbackExecutor;
	}
	
	public void setServerSSL(final SslContext context, final SSLCLientCertReq reqCertificate, final String... tlsVersions) {
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
	
	public void setClientSSL(final SslContext context) {
		if(role.equals(CommunicationRole.SERVER)) {
			this.isSecure = true;
			this.sslContext = context;
		}
		else {
			throw new IllegalArgumentException("Cannot set Client side security on a TCP Server");
		}
	}
	
	public boolean isSecured() {
		return isSecure;
	}
	
	public SSLCLientCertReq getSslClientCertificateRequestLevel() {
		return reqCertificate;
	}
	
	public String[] getTLSVersions() {
		return tlsVersion;
	}
	
	public SslContext getSslContext() {
		return sslContext;
	}

}
