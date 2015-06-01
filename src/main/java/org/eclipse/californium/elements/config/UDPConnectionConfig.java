package org.eclipse.californium.elements.config;

public class UDPConnectionConfig extends ConnectionConfig{

	@Override
	public LayerSemantic getTransportLayer() {
		return LayerSemantic.UDP;
	}

	@Override
	public CommunicationRole getCommunicationRole() {
		return CommunicationRole.NODE;
	}


	
}
