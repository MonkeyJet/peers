/*
    This file is part of Peers.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008 Yohann Martineau 
*/

package net.sourceforge.peers.sip.core.useragent;

import java.util.Hashtable;

import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;
import net.sourceforge.peers.sip.transaction.ClientTransaction;
import net.sourceforge.peers.sip.transaction.ClientTransactionUser;
import net.sourceforge.peers.sip.transaction.Transaction;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.TransportManager;


public class MidDialogRequestManager extends RequestManager
        implements ClientTransactionUser {

    public MidDialogRequestManager(UserAgent userAgent,
            TransactionManager transactionManager,
            TransportManager transportManager) {
        super(userAgent, transactionManager, transportManager);
    }

    ////////////////////////////////////////////////
    // methods for UAC
    ////////////////////////////////////////////////

    public void generateMidDialogRequest(Dialog dialog,
            String method) {
        

        SipRequest subRequest = dialog.buildSubsequentRequest(RFC3261.METHOD_BYE);

        if (RFC3261.METHOD_BYE.equals(method)) {
            byeHandler.preprocessBye(subRequest, dialog);
        }
        //TODO check that subsequent request is supported before client
        //transaction creation
        if (!RFC3261.METHOD_INVITE.equals(method)) {
            ClientTransaction clientTransaction = createNonInviteClientTransaction(subRequest,
                    null);
            if (clientTransaction != null) {
                clientTransaction.start();
            }
        } else {
            //TODO client transaction user is managed by invite handler directly
        }

        
    }
    
    
    public ClientTransaction createNonInviteClientTransaction(
            SipRequest sipRequest, String branchId) {
        //8.1.2
        SipURI requestUri = sipRequest.getRequestUri();

        //TODO if header route is present, addrspec = toproute.nameaddress.addrspec
        String transport = RFC3261.TRANSPORT_UDP;
        Hashtable<String, String> params = requestUri.getUriParameters();
        if (params != null) {
            String reqUriTransport = params.get(RFC3261.PARAM_TRANSPORT);
            if (reqUriTransport != null) {
                transport = reqUriTransport; 
            }
        }
        int port = requestUri.getPort();
        if (port == SipURI.DEFAULT_PORT) {
            port = RFC3261.TRANSPORT_DEFAULT_PORT;
        }
        ClientTransaction clientTransaction = transactionManager
            .createClientTransaction(sipRequest,
                    requestUri.getHost(), port, transport, branchId, this,
                    transportManager);
        return clientTransaction;
    }
    
    
    
    
    

    
    
    
    
    
    ////////////////////////////////////////////////
    // methods for UAS
    ////////////////////////////////////////////////
    //why static ????
    public static SipResponse generateMidDialogResponse(SipRequest sipRequest,
            Dialog dialog, int statusCode, String reasonPhrase) {
        //8.2.6.2
        SipResponse sipResponse = new SipResponse(statusCode, reasonPhrase);
        SipHeaders requestHeaders = sipRequest.getSipHeaders();
        SipHeaders responseHeaders = sipResponse.getSipHeaders();
        SipHeaderFieldName fromName = new SipHeaderFieldName(RFC3261.HDR_FROM);
        responseHeaders.add(fromName, requestHeaders.get(fromName));
        SipHeaderFieldName callIdName = new SipHeaderFieldName(RFC3261.HDR_CALLID);
        responseHeaders.add(callIdName, requestHeaders.get(callIdName));
        SipHeaderFieldName cseqName = new SipHeaderFieldName(RFC3261.HDR_CSEQ);
        responseHeaders.add(cseqName, requestHeaders.get(cseqName));
        SipHeaderFieldName viaName = new SipHeaderFieldName(RFC3261.HDR_VIA);
        responseHeaders.add(viaName, requestHeaders.get(viaName));//TODO check ordering
        SipHeaderFieldName toName = new SipHeaderFieldName(RFC3261.HDR_TO);
        SipHeaderFieldValue toValue = requestHeaders.get(toName);
        SipHeaderParamName toTagParamName = new SipHeaderParamName(RFC3261.PARAM_TAG);
        String toTag = toValue.getParam(toTagParamName);
        if (toTag == null) {
            toTag = dialog.getLocalTag();
            toValue.addParam(toTagParamName, toTag);
        }
        responseHeaders.add(toName, toValue);
        return sipResponse;
    }

    public void manageMidDialogRequest(SipRequest sipRequest, Dialog dialog) {

        if (dialog.getRemoteCSeq() == Dialog.EMPTY_CSEQ) {
            SipHeaders sipHeaders = sipRequest.getSipHeaders();
            SipHeaderFieldValue cseq =
                sipHeaders.get(new SipHeaderFieldName(RFC3261.HDR_CSEQ));
            String cseqStr = cseq.getValue();
            int pos = cseqStr.indexOf(' ');
            if (pos < 0) {
                pos = cseqStr.indexOf('\t');
            }
            dialog.setRemoteCSeq(Integer.parseInt(cseqStr.substring(0, pos)));
        }
        
        if (RFC3261.METHOD_BYE.equals(sipRequest.getMethod())) {
            byeHandler.handleBye(sipRequest, dialog);
            
        }
    }

    ///////////////////////////////////////
    //ClientTransaction methods
    ///////////////////////////////////////
    public void errResponseReceived(SipResponse sipResponse) {
        // TODO Auto-generated method stub
        
    }


    public void provResponseReceived(SipResponse sipResponse, Transaction transaction) {
        // TODO Auto-generated method stub
        
    }


    public void successResponseReceived(SipResponse sipResponse, Transaction transaction) {
        // TODO Auto-generated method stub
        
    }


    public void transactionTimeout() {
        // TODO Auto-generated method stub
        
    }


    public void transactionTransportError() {
        // TODO Auto-generated method stub
        
    }
}
