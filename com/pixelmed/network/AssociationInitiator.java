/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

//import com.pixelmed.transfermonitor.MonitoredInputStream;
//import com.pixelmed.transfermonitor.MonitoredOutputStream;
//import com.pixelmed.transfermonitor.TransferMonitoringContext;

import com.pixelmed.utils.ByteArray;
import com.pixelmed.utils.HexDump;

import java.util.LinkedList;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.Arrays;

/**
 * @author	dclunie
 */
class AssociationInitiator extends Association {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AssociationInitiator.java,v 1.25 2008/09/24 18:54:52 dclunie Exp $";

	private String hostname;
	private int port;

	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Maximum PDU Size of the toolkit is used.
	 *
	 * The open association is left in state 6 - Data Transfer.
	 *
	 * @param	hostname			hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port				TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle			the AE Title of the remote (their) end of the association
	 * @param	callingAETitle			the AE Title of the local (our) end of the association
	 * @param	implementationClassUID		the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
	 * @param	implementationVersionName	the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects,
	 *						each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects,
	 *						each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport		true if to use secure transport protocol
	 * @param	username			may be null if no user identity
	 * @param	password			may be null if no user identity or no password required
	 * @param	debugLevel			0 for no debugging, > 0 for increasingly verbose debugging
	 * @exception	IOException
	 * @exception	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	protected AssociationInitiator(String hostname,int port,String calledAETitle,
				String callingAETitle, String implementationClassUID, String implementationVersionName,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport,String username,String password,
				int debugLevel) throws DicomNetworkException, IOException {
		super(debugLevel);
		this.hostname=hostname;
		this.port=port;
		this.calledAETitle=calledAETitle;
		this.callingAETitle=callingAETitle;
		this.presentationContexts=presentationContexts;
		this.scuSCPRoleSelections=scuSCPRoleSelections;
		
		int userIdentityType = 0;
		String userIdentityPrimaryField = null;
		String userIdentitySecondaryField = null;
		if (username != null) {
			userIdentityPrimaryField = username;
			if (password != null) {
				userIdentitySecondaryField = password;
				userIdentityType = 2;
			}
			else {
				userIdentityType = 1;
			}
		}
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: userIdentityType="+userIdentityType);
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: OurMaximumLengthReceived="+ourMaximumLengthReceived);
		AssociateRequestPDU arq = new AssociateRequestPDU(calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,
				ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,userIdentityType,userIdentityPrimaryField,userIdentitySecondaryField);
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Us:\n"+arq);
												// State 1 - Idle
		//TransferMonitoringContext inputTransferMonitoringContext  = new TransferMonitoringContext("Association["+associationNumber+"] Initiator Read  "+callingAETitle+"<-"+calledAETitle);
		//TransferMonitoringContext outputTransferMonitoringContext = new TransferMonitoringContext("Association["+associationNumber+"] Initiator Wrote "+callingAETitle+"->"+calledAETitle);
		try {
												// AE-1    - Issue TP Connect Primitive
			if (secureTransport) {
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
				SSLSocket sslsocket = (SSLSocket)sslsocketfactory.createSocket(hostname,port);
				String[] suites = getCipherSuitesToEnable(sslsocket.getSupportedCipherSuites());	
				if (suites != null) {
					sslsocket.setEnabledCipherSuites(suites);
				}
				String[] protocols = getProtocolsToEnable(sslsocket.getEnabledProtocols());
				if (protocols != null) {
					sslsocket.setEnabledProtocols(protocols);
				}
				socket = sslsocket;
			}
			else {
				socket = new Socket(hostname,port);
			}
			
			setSocketOptions(socket,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,debugLevel);
												// State 4 - Awaiting TP open to complete
												//         - Transport connection confirmed 
			in = socket.getInputStream();
			//in = new MonitoredInputStream(socket.getInputStream(),inputTransferMonitoringContext);
			out = socket.getOutputStream();
			//out = new MonitoredOutputStream(socket.getOutputStream(),outputTransferMonitoringContext);
			
			out.write(arq.getBytes());						// AE-2     - Send A-ASSOCIATE-RQ PDU
			out.flush();
												// State 5  - Awaiting A-ASSOCIATE-AC or -RJ PDU
			byte[] startBuffer =  new byte[6];
			//in.read(startBuffer,0,6);	// block for type and length of PDU
			readInsistently(in,startBuffer,0,6,"type and length of PDU");
			int pduType = startBuffer[0]&0xff;
			int pduLength = ByteArray.bigEndianToUnsignedInt(startBuffer,2,4);

if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Them: PDU Type: 0x"+Integer.toHexString(pduType)+" (length 0x"+Integer.toHexString(pduLength)+")");

			if (pduType == 0x02) {							//           - A-ASSOCIATE-AC PDU
				AssociateAcceptPDU aac = new AssociateAcceptPDU(getRestOfPDU(in,startBuffer,pduLength));
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Them:\n"+aac);
				this.presentationContexts=aac.getAcceptedPresentationContextsWithAbstractSyntaxIncludedFromRequest(this.presentationContexts);
				
				// this.maximumLengthReceived is used to set the size of what we send ... don't let what they
				// are capable of receiving exceed what we can fit in our socket send buffer ...
				if (this.maximumLengthReceived == 0 || aac.getMaximumLengthReceived() < this.maximumLengthReceived) {
					this.maximumLengthReceived=aac.getMaximumLengthReceived();
				}
if (debugLevel > 0) System.err.println("Association["+associationNumber+"]: We will send them PDUs of: "+this.maximumLengthReceived);

if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Accepted presentation contexts:\n"+this.presentationContexts);
												// AE-3      - issue confirmation indication (i.e. do nothing but return)
												// State 6   - Data Transfer
			}
			else if (pduType == 0x03) {						//           - A-ASSOCIATE-RJ PDU
				AssociateRejectPDU arj = new AssociateRejectPDU(getRestOfPDU(in,startBuffer,pduLength));
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Them:\n"+arj);
				socket.close();							// AE-4      - Close transport connection and indicate rejection
				//inputTransferMonitoringContext.close();
				//outputTransferMonitoringContext.close();
				throw new DicomNetworkException("A-ASSOCIATE-RJ indication - "+arj.getInfo());
												// State 1   - Idle
			}
			else if (pduType == 0x07) {						//           - A-ABORT PDU
				AAbortPDU aab = new AAbortPDU(getRestOfPDU(in,startBuffer,pduLength));
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Them:\n"+aab);
				socket.close();							// AA-3      - Close transport connection and indicate abort
				//inputTransferMonitoringContext.close();
				//outputTransferMonitoringContext.close();
				throw new DicomNetworkException("A-ABORT indication - "+aab.getInfo());
												// State 1   - Idle
			}
			else {									//           - Invalid or unrecognized PDU received
if (debugLevel > 1) System.err.println("Association["+associationNumber+"]: Aborting");

				AAbortPDU aab = new AAbortPDU(2,2);				// AA-8      - Send A-ABORT PDU (service provider source, unexpected PDU)
				out.write(aab.getBytes());
				out.flush();
												//             issue an A-P-ABORT indication and start ARTIM
												// State 13  - Awaiting Transport connection close
				// should wait for ARTIM but ...
				socket.close();
				//inputTransferMonitoringContext.close();
				//outputTransferMonitoringContext.close();
				throw new DicomNetworkException("A-P-ABORT indication - "+aab.getInfo());
												// State 1   - Idle
			}
		}
		catch (IOException e) {								//           - Transport connection closed (or other error)
			//inputTransferMonitoringContext.close();
			//outputTransferMonitoringContext.close();
			throw new DicomNetworkException("A-P-ABORT indication - ("+hostname+":"+Integer.toString(port)+") - "+e);		// AA-4      - indicate A-P-ABORT
												// State 1   - Idle
		}

		// falls through only from State 6 - Data Transfer
	}

	/*
	 * Returns a string representation of the object.
	 *
	 * @return	a string representation of the object
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Association["+associationNumber+"]: Hostname: "); sb.append(hostname); sb.append("\n");
		sb.append("Association["+associationNumber+"]: Port: "); sb.append(port); sb.append("\n");
		sb.append(super.toString());
		return sb.toString();
	}
}


