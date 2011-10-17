package org.ohmage.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.domain.campaign.SurveyResponse;
import org.ohmage.util.StringUtils;

/**
 * This class represents a server's configuration including its name, version,
 * and state values.
 * 
 * @author John Jenkins
 */
public class ServerConfig {
	/**
	 * The key to use when creating/reading JSON for the application's name.
	 */
	public static final String JSON_KEY_APPLICATION_NAME = "application_name";
	/**
	 * The key to use when creating/reading JSON for the application's version.
	 */
	public static final String JSON_KEY_APPLICATION_VERSION = "application_version";
	/**
	 * The key to use when creating/reading JSON for the application's build.
	 */
	public static final String JSON_KEY_APPLICATION_BUILD = "application_build";
	/**
	 * The key to use when creating/reading JSON for the default survey 
	 * response privacy state for newly uploaded survey responses.
	 */
	public static final String JSON_KEY_DEFAULT_SURVEY_RESPONSE_PRIVACY_STATE = "default_survey_response_sharing_state";
	/**
	 * The key to use when creating/reading JSON for the list of all survey
	 * response privacy states.
	 */
	public static final String JSON_KEY_SURVEY_RESPONSE_PRIVACY_STATES = "survey_response_privacy_states";
	
	private final String appName;
	private final String appVersion;
	private final String appBuild;
	private final SurveyResponse.PrivacyState defaultSurveyResponsePrivacyState;
	private final SurveyResponse.PrivacyState[] surveyResponsePrivacyStates;
	
	/**
	 * Creates a new server configuration.
	 * 
	 * @param appName The application's name.
	 * 
	 * @param appVersion The application's version.
	 * 
	 * @param appBuild The applications build.
	 * 
	 * @param defaultSurveyResponsePrivacyState The default survey response
	 * 											privacy state for newly 
	 * 											uploaded survey responses.
	 * 
	 * @param surveyResponsePrivacyStates An array of all of the survey 
	 * 									  response privacy states.
	 * 
	 * @throws IllegalArgumentException Thrown if any of the values are invalid
	 * 									or null.
	 */
	public ServerConfig(final String appName, final String appVersion,
			final String appBuild, 
			final SurveyResponse.PrivacyState defaultSurveyResponsePrivacyState,
			final SurveyResponse.PrivacyState[] surveyResponsePrivacyStates) {
		
		if(StringUtils.isEmptyOrWhitespaceOnly(appName)) {
			throw new IllegalArgumentException("The application name is null or whitespace only.");
		}
		else if(StringUtils.isEmptyOrWhitespaceOnly(appVersion)) {
			throw new IllegalArgumentException("The application version is null or whitespace only.");
		}
		else if(StringUtils.isEmptyOrWhitespaceOnly(appBuild)) {
			throw new IllegalArgumentException("The application build is null or whitespace only.");
		}
		else if(defaultSurveyResponsePrivacyState == null) {
			throw new IllegalArgumentException("The default survey response privacy state is null.");
		}
		else if(surveyResponsePrivacyStates == null) {
			throw new IllegalArgumentException("The list of default survey response privacy states is null.");
		}
		
		this.appName = appName;
		this.appVersion = appVersion;
		this.appBuild = appBuild;
		this.defaultSurveyResponsePrivacyState = defaultSurveyResponsePrivacyState;
		
		this.surveyResponsePrivacyStates = 
			new SurveyResponse.PrivacyState[surveyResponsePrivacyStates.length];
		for(int i = 0; i < surveyResponsePrivacyStates.length; i++) {
			this.surveyResponsePrivacyStates[i] = surveyResponsePrivacyStates[i];
		}
	}
	
	/**
	 * Creates a new server configuration from a JSONObject.
	 * 
	 * @param serverConfigAsJson The information about the server as 
	 * 							 JSONObject.
	 * 
	 * @throws IllegalArgumentException Thrown if the JSONObject is null or if
	 * 									it is missing any of the required keys.
	 */
	public ServerConfig(final JSONObject serverConfigAsJson) {
		if(serverConfigAsJson == null) {
			throw new IllegalArgumentException("The server configuration JSON is null.");
		}
		
		try {
			appName = serverConfigAsJson.getString(JSON_KEY_APPLICATION_NAME);
		}
		catch(JSONException e) {
			throw new IllegalArgumentException("The application name was missing from the JSON.", e);
		}
		
		try {
			appVersion = serverConfigAsJson.getString(JSON_KEY_APPLICATION_VERSION);
		}
		catch(JSONException e) {
			throw new IllegalArgumentException("The application version was missing from the JSON.", e);
		}
		
		try {
			appBuild = serverConfigAsJson.getString(JSON_KEY_APPLICATION_BUILD);
		}
		catch(JSONException e) {
			throw new IllegalArgumentException("The application name was missing from the JSON.", e);
		}
		
		try {
			defaultSurveyResponsePrivacyState = 
				SurveyResponse.PrivacyState.getValue(
						serverConfigAsJson.getString(JSON_KEY_DEFAULT_SURVEY_RESPONSE_PRIVACY_STATE)
					);
		}
		catch(JSONException e) {
			throw new IllegalArgumentException("The application name was missing from the JSON.", e);
		}
		catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("The default survey response privacy state is not a known survey response privacy state.", e);
		}
		
		try {
			JSONArray surveyResponsePrivacyStatesJson = 
				serverConfigAsJson.getJSONArray(JSON_KEY_SURVEY_RESPONSE_PRIVACY_STATES);
			
			int numPrivacyStates = surveyResponsePrivacyStatesJson.length();
			surveyResponsePrivacyStates = 
				new SurveyResponse.PrivacyState[numPrivacyStates];
			
			for(int i = 0; i < numPrivacyStates; i++) {
				surveyResponsePrivacyStates[i] = 
					SurveyResponse.PrivacyState.getValue(
							surveyResponsePrivacyStatesJson.getString(i)
						);
			}
		}
		catch(JSONException e) {
			throw new IllegalArgumentException("The application name was missing from the JSON.", e);
		}
	}
	
	/**
	 * Returns the application's name.
	 * 
	 * @return The application's name.
	 */
	public final String getAppName() {
		return appName;
	}
	
	/**
	 * Returns the application's version.
	 * 
	 * @return The application's version.
	 */
	public final String getAppVersion() {
		return appVersion;
	}
	
	/**
	 * Returns the application's build.
	 * 
	 * @return The application's build.
	 */
	public final String getAppBuild() {
		return appBuild;
	}
	
	/**
	 * Returns the default survey response privacy state for newly uploaded
	 * survey responses.
	 * 
	 * @return The default survey response privacy state.
	 */
	public final SurveyResponse.PrivacyState getDefaultSurveyResponsePrivacyState() {
		return defaultSurveyResponsePrivacyState;
	}
	
	/**
	 * Returns an array of all of the survey response privacy states.
	 * 
	 * @return An array of all of the survey response privacy states.
	 */
	public final SurveyResponse.PrivacyState[] getSurveyResponsePrivacyStates() {
		SurveyResponse.PrivacyState[] result = 
			new SurveyResponse.PrivacyState[surveyResponsePrivacyStates.length];
		
		for(int i = 0; i < surveyResponsePrivacyStates.length; i++) {
			result[i] = surveyResponsePrivacyStates[i];
		}
		
		return result;
	}
	
	/**
	 * Returns this server configuration as a JSONObject.
	 * 
	 * @return This server configuration as a JSONObject.
	 * 
	 * @throws IllegalStateException Thrown if there is an error building the
	 * 								 JSONObject.
	 */
	public JSONObject toJson() {
		try {
			JSONObject result = new JSONObject();
			
			result.put(JSON_KEY_APPLICATION_NAME, appName);
			result.put(JSON_KEY_APPLICATION_VERSION, appVersion);
			result.put(JSON_KEY_APPLICATION_BUILD, appBuild);
			result.put(JSON_KEY_DEFAULT_SURVEY_RESPONSE_PRIVACY_STATE, defaultSurveyResponsePrivacyState);
			result.put(JSON_KEY_SURVEY_RESPONSE_PRIVACY_STATES, new JSONArray(surveyResponsePrivacyStates));
			
			return result;
		}
		catch(JSONException e) {
			throw new IllegalStateException("There was an error creating the JSONObject.", e);
		}
	}
}