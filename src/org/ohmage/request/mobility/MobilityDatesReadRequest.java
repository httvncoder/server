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
package org.ohmage.request.mobility;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.cache.PreferenceCache;
import org.ohmage.exception.CacheMissException;
import org.ohmage.exception.InvalidRequestException;
import org.ohmage.exception.ServiceException;
import org.ohmage.exception.ValidationException;
import org.ohmage.request.InputKeys;
import org.ohmage.request.UserRequest;
import org.ohmage.service.MobilityServices;
import org.ohmage.service.UserClassServices;
import org.ohmage.service.UserServices;
import org.ohmage.util.StringUtils;
import org.ohmage.util.DateTimeUtils;
import org.ohmage.validator.MobilityValidators;
import org.ohmage.validator.UserValidators;

/**
 * Queries the system for all of the dates within an optional bound that the
 * user created a Mobility point.<br />
 * <br />
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
 *     <td>{@value org.ohmage.request.InputKeys#START_DATE}</td>
 *     <td>The first date to aggregate the results.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#END_DATE}</td>
 *     <td>The last date to aggregate results.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#USERNAME}</td>
 *     <td>The username of the user for whom the data is desired. If omitted,
 *       the requesting user is used.</td>
 *     <td>false</td>
 *   </tr>
 * </table>
 * 
 * @author John Jenkins
 *
 */
public class MobilityDatesReadRequest extends UserRequest {
	private static final Logger LOGGER = Logger.getLogger(MobilityDatesReadRequest.class);

	private final DateTime startDate;
	private final DateTime endDate;
	private final String username;
	
	private Collection<DateTime> dates;
	
	/**
	 * Creates a new Mobility dates read request.
	 * 
	 * @param httpRequest The HttpServletRequest generated by the servlet
	 * 					  container and used to populate and fulfil this 
	 * 					  request.
	 * 
	 * @throws InvalidRequestException Thrown if the parameters cannot be 
	 * 								   parsed.
	 * 
	 * @throws IOException There was an error reading from the request.
	 */
	public MobilityDatesReadRequest(HttpServletRequest httpRequest) throws IOException, InvalidRequestException {
		super(httpRequest, false, TokenLocation.EITHER, null);
		
		DateTime tStartDate = new DateTime(0);
		DateTime tEndDate = new DateTime();
		String tUsername = null;
		
		if(! isFailed()) {
			LOGGER.info("Creating a Mobility dates read request.");
			
			try {
				String[] t;
				
				// Get the start date.
				t = getParameterValues(InputKeys.START_DATE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_DATE, 
							"Multiple start dates were given: " + 
									InputKeys.START_DATE);
				}
				else if(t.length == 1) {
					tStartDate = MobilityValidators.validateDate(t[0]);
					
					if(tStartDate == null) {
						tStartDate = new DateTime(0);
					}
				}
				
				// Get the end date.
				t = getParameterValues(InputKeys.END_DATE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_DATE, 
							"Multiple end dates were given: " + 
									InputKeys.END_DATE);
				}
				else if(t.length == 1) {
					tEndDate = MobilityValidators.validateDate(t[0]);
					
					if(tEndDate == null) {
						tEndDate = new DateTime();
					}
				}
				
				t = getParameterValues(InputKeys.USERNAME);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.USER_INVALID_USERNAME, 
							"Multiple usernames to query were given: " + 
									InputKeys.USERNAME);
				}
				else if(t.length == 1) {
					tUsername = UserValidators.validateUsername(t[0]);
				}
			}
			catch(ValidationException e) {
				e.failRequest(this);
				e.logException(LOGGER);
			}
		}
		
		startDate = tStartDate;
		endDate = tEndDate;
		username = tUsername;
		
		dates = Collections.emptyList();
	}

	/**
	 * Services the request.
	 */
	@Override
	public void service() {
		LOGGER.info("Servicing the Mobility dates read request.");
		
		if(! authenticate(AllowNewAccount.NEW_ACCOUNT_DISALLOWED)) {
			return;
		}
		
		try {
			if((username != null) && (! username.equals(getUser().getUsername()))) {
				boolean isPlausible;
				try {
					isPlausible = 
							StringUtils.decodeBoolean(
									PreferenceCache.instance().lookup(
											PreferenceCache.KEY_PRIVILEGED_USER_IN_CLASS_CAN_VIEW_MOBILITY_FOR_EVERYONE_IN_CLASS));
				}
				catch(CacheMissException e) {
					throw new ServiceException(e);
				}
				
				try {
					UserServices.instance().verifyUserIsAdmin(
							getUser().getUsername());
				}
				catch(ServiceException notAdmin) {
					if(isPlausible) {
						UserClassServices
							.instance()
							.userIsPrivilegedInAnotherUserClass(
									getUser().getUsername(), 
									username);
					}
					else {
						throw new ServiceException(
								ErrorCode.MOBILITY_INSUFFICIENT_PERMISSIONS,
								"A user is not allowed to query Mobility information about another user.");
					}
				}
				
				UserServices.instance().checkUserExistance(username, true);
			}
			
			LOGGER.info("Gathering the dates.");
			dates = 
					MobilityServices.instance().getDates(
							startDate, 
							endDate, 
							(username == null) ? getUser().getUsername() : username);
		}
		catch(ServiceException e) {
			e.failRequest(this);
			e.logException(LOGGER);
		}
	}

	/**
	 * Converts the list of dates to a JSONArray and returns it to the caller.
	 */
	@Override
	public void respond(
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {
		
		LOGGER.info("Responding to the Mobiltiy dates read request.");

		JSONArray resultJson = new JSONArray();
		
		for(DateTime date : dates) {
			resultJson.put(DateTimeUtils.getIso8601DateString(date, false));
		}
		
		respond(httpRequest, httpResponse, JSON_KEY_DATA, resultJson);
	}

}
