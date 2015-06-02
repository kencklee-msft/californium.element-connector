package org.eclipse.californium.elements.config;

public abstract class UDPConnectionConfig extends ConnectionConfig{

	@Override
	public final LayerSemantic getTransportLayer() {
		return LayerSemantic.UDP;
	}

	@Override
	public final CommunicationRole getCommunicationRole() {
		return CommunicationRole.NODE;
	}


	
}
