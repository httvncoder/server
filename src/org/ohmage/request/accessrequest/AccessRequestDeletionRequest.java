/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.request.accessrequest;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.exception.InvalidRequestException;
import org.ohmage.exception.ServiceException;
import org.ohmage.exception.ValidationException;
import org.ohmage.request.InputKeys;
import org.ohmage.request.UserRequest;
import org.ohmage.service.AccessRequestServices;
import org.ohmage.domain.AccessRequest;

/**
 * <p>Delete AccessRequest. </p>
 * <table border="1">
 *   <tr>
 *     <td>Parameter Name</td>
 *     <td>Description</td>
 *     <td>Required</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CLIENT}</td>
 *     <td>A string describing the client that is making this request.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#USER_ACCESS_REQUEST_ID_LIST}</td>
 *     <td>The list of user setup request UUIDs.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#USER_ACCESS_REQUEST_CONTENT}</td>
 *     <td>The content of the user setup request e.g. project name, objectives, etc.</td>
 *     <td>true</td>
 *   </tr>
 * </table>
 * 
 * @author Hongsuda T.
 */
public class AccessRequestDeletionRequest extends UserRequest {
	private static final Logger LOGGER = Logger.getLogger(AccessRequestDeletionRequest.class);
	
	private final Collection<String> requestIdList;
	
	/**
	 * Creates a user creation request.
	 * 
	 * @param httpRequest The HttpServletRequest that contains the required and
	 * 					  optional parameters for creating this request.
	 * 
	 * @throws InvalidRequestException Thrown if the parameters cannot be 
	 * 								   parsed.
	 * 
	 * @throws IOException There was an error reading from the request.
	 */
	public AccessRequestDeletionRequest(HttpServletRequest httpRequest) throws IOException, InvalidRequestException {
		super(httpRequest, null, TokenLocation.PARAMETER, null);

		Collection<String> tRequestIdList = null;
		
		if(! isFailed()) {
			LOGGER.info("Creating a AccessRequestDeletionRequest.");
		
			try {
				String[] t;
				
				// request's uuid (optional)
				t = getParameterValues(InputKeys.USER_ACCESS_REQUEST_ID_LIST);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.USER_ACCESS_REQUEST_INVALID_PRAMETER,
							"Multiple request_id parameters were given: " +
								InputKeys.USER_ACCESS_REQUEST_ID_LIST);
				}
				else if(t.length == 1) {
					tRequestIdList = AccessRequest.validateRequestIdList(t[0]);
				}
				else {
					throw new ValidationException(
							ErrorCode.USER_ACCESS_REQUEST_INVALID_PRAMETER,
							"Missing parameter: " +
								InputKeys.USER_ACCESS_REQUEST_ID_LIST);
				}			
				
			}
			catch(ValidationException e) {
				e.failRequest(this);
				e.logException(LOGGER);
			}
		}
		
		requestIdList = tRequestIdList;
	}

	
	/**
	 * Services this request if an existing user is making the request.
	 */
	@Override
	public void service() {
		LOGGER.info("Servicing the AccessRequestDeletionRequest.");
		
		if(! authenticate(AllowNewAccount.NEW_ACCOUNT_DISALLOWED)) {
			return;
		}
		
		try {
			LOGGER.info("Delete the AccessRequest.");
			
			// all validations are done in deleteUserSetupRequests
			AccessRequestServices.instance().deleteAccessRequests(requestIdList, this.getUser().getUsername());
			
			// TODO: if successful, send an email notification
			
		}
		catch(ServiceException e) {
			e.failRequest(this);
			e.logException(LOGGER);
		}
	}

	/**
	 * Responds with success or failure and a message.
	 */
	@Override
	public void respond(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		super.respond(httpRequest, httpResponse, (JSONObject) null);
	}
}