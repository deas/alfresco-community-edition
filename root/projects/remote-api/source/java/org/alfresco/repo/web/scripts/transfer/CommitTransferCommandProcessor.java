/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.repo.web.scripts.transfer;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.service.cmr.transfer.TransferException;
import org.alfresco.service.cmr.transfer.TransferReceiver;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.json.JSONWriter;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

/**
 * This command processor is used to record the start a transfer. No other transfer can be started after this command
 * has executed until the started transfer terminates.
 * 
 * @author brian
 * 
 */
public class CommitTransferCommandProcessor implements CommandProcessor
{
    private static final String MSG_CAUGHT_UNEXPECTED_EXCEPTION = "transfer_service.receiver.caught_unexpected_exception";
    
    private static Log logger = LogFactory.getLog(CommitTransferCommandProcessor.class);

    private TransferReceiver receiver;

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.web.scripts.transfer.CommandProcessor#process(org.alfresco .web.scripts.WebScriptRequest,
     * org.alfresco.web.scripts.WebScriptResponse)
     */
    public int process(WebScriptRequest req, WebScriptResponse resp)
    {   

        //Read the transfer id from the request
        HttpServletRequest servletRequest = ((WebScriptServletRequest)req).getHttpServletRequest();
        String transferId = servletRequest.getParameter("transferId");

        if ((transferId == null))
        {
            logger.debug("transferId is missing");
            resp.setStatus(Status.STATUS_BAD_REQUEST);
            return Status.STATUS_BAD_REQUEST;
        }
        
        try
        {   
            receiver.commit(transferId);

            // return the unique transfer id (the lock id)
            StringWriter stringWriter = new StringWriter(300);
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            jsonWriter.startObject();
            jsonWriter.writeValue("transferId", transferId);
            jsonWriter.endObject();
            String response = stringWriter.toString();
            
            resp.setContentType("application/json");
            resp.setContentEncoding("UTF-8");
            int length = response.getBytes("UTF-8").length;
            resp.addHeader("Content-Length", "" + length);
            resp.setStatus(Status.STATUS_OK);
            resp.getWriter().write(response);
            
            logger.debug("committed transferId:" + transferId);
            
            return Status.STATUS_OK;
        } 
        catch (Exception ex)
        {
            logger.debug("caught exception :" + ex.toString(), ex);
            
            //TODO Think very carefully about this.    
            //TODO MER I think we need to roll forward here since we are after prepare.
            receiver.end(transferId);
            if (ex instanceof TransferException)
            {
                throw (TransferException) ex;
            }
            throw new TransferException(MSG_CAUGHT_UNEXPECTED_EXCEPTION, ex);
        }
    }

    /**
     * @param receiver the receiver to set
     */
    public void setReceiver(TransferReceiver receiver)
    {
        this.receiver = receiver;
    }

    
}
