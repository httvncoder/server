package org.ohmage.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.domain.campaign.Campaign;
import org.ohmage.domain.campaign.Prompt;
import org.ohmage.domain.campaign.SurveyResponse;
import org.ohmage.exception.DataAccessException;
import org.ohmage.exception.ErrorCodeException;
import org.ohmage.util.StringUtils;
import org.ohmage.util.TimeUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * This class is responsible for creating, reading, updating, and deleting
 * survey responses.
 * 
 * @author John Jenkins
 * @author Joshua Selsky
 */
public class SurveyResponseQueries extends Query {
	// Retrieves the ID for all survey responses in a campaign.
	private static final String SQL_GET_IDS_FOR_CAMPAIGN =
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id";
	
	// Retrieves the survey response ID for all of the survey responses made by
	// a given user in a given campaign.
	private static final String SQL_GET_IDS_FOR_USER = 
		"SELECT sr.id " +
		"FROM user u, campaign c, survey_response sr " +
		"WHERE u.username = ? " +
		"AND u.id = sr.user_id " +
		"AND c.urn = ? " +
		"AND c.id = sr.campaign_id";
	
	// Retrieves the survey response ID for all of the survey responses made by
	// a given client in a given campaign.
	private static final String SQL_GET_IDS_WITH_CLIENT = 
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND sr.client = ?";
	
	// Retrieves the survey response ID for all of the survey responses made on 
	// or after some date in a given campaign.
	private static final String SQL_GET_IDS_AFTER_DATE =
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND sr.msg_timestamp >= ?";
	
	// Retrieves the survey response ID for all of the survey responses made on 
	// or before some date in a given campaign.
	private static final String SQL_GET_IDS_BEFORE_DATE =
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND sr.msg_timestamp <= ?";
	
	// Retrieves the survey response ID for all of the survey responses with a
	// given privacy state in a given campaign.
	private static final String SQL_GET_IDS_WITH_PRIVACY_STATE = 
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr, survey_response_privacy_state srps " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND srps.privacy_state = ? " +
		"AND srps.id = sr.privacy_state_id";
	
	// Retrieves the survey response ID for all of the survey responses with a
	// given survey ID in a given campaign.
	private static final String SQL_GET_IDS_WITH_SURVEY_ID = 
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND sr.survey_id = ?";
	
	// Retrieves the survey response ID for all of the survey responses with a
	// given prompt ID in a given campaign.
	private static final String SQL_GET_IDS_WITH_PROMPT_ID = 
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr, prompt_response pr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND pr.prompt_id = ? " +
		"AND sr.id = pr.survey_response_id";
	
	// Retrieves the survey response ID for all of the survey responses with a
	// given prompt type in a given campaign.
	private static final String SQL_GET_IDS_WITH_PROMPT_TYPE = 
		"SELECT sr.id " +
		"FROM campaign c, survey_response sr, prompt_response pr " +
		"WHERE c.urn = ? " +
		"AND c.id = sr.campaign_id " +
		"AND pr.prompt_type = ? " +
		"AND sr.id = pr.survey_response_id";
	
	// Retrieves all of the information about a single survey response.
	private static final String SQL_GET_SURVEY_RESPONSE = 
		"SELECT u.username, c.urn, sr.id, sr.client, sr.msg_timestamp, sr.epoch_millis, sr.phone_timezone, sr.survey_id, sr.launch_context, sr.location_status, sr.location, srps.privacy_state " +
		"FROM user u, campaign c, survey_response sr, survey_response_privacy_state srps " +
		"WHERE sr.id = ? " +
		"AND u.id = sr.user_id " +
		"AND c.id = sr.campaign_id " +
		"AND srps.id = sr.privacy_state_id";
	
	// Retrieves all of the information about a single survey response.
	private static final String SQL_GET_SURVEY_RESPONSES = 
		"SELECT u.username, c.urn, sr.id, sr.client, " +
			"sr.msg_timestamp, sr.epoch_millis, sr.phone_timezone, " +
			"sr.survey_id, sr.launch_context, " +
			"sr.location_status, sr.location, srps.privacy_state " +
		"FROM user u, campaign c, survey_response sr, survey_response_privacy_state srps " +
		"WHERE u.id = sr.user_id " +
		"AND c.id = sr.campaign_id " +
		"AND srps.id = sr.privacy_state_id " +
		"AND sr.id in ";
	
	// Retrieves all of the information about all prompt responses that pertain
	// to a single survey response.
	private static final String SQL_GET_PROMPT_RESPONSES = 
		"SELECT prompt_id, prompt_type, repeatable_set_id, repeatable_set_iteration, response " +
		"FROM prompt_response " +
		"WHERE survey_response_id = ?";
	
	// Updates a survey response's privacy state.
	private static final String SQL_UPDATE_SURVEY_RESPONSE_PRIVACY_STATE = 
		"UPDATE survey_response " +
		"SET privacy_state_id = (SELECT id FROM survey_response_privacy_state WHERE privacy_state = ?) " +
		"WHERE id = ?";
	
	// Deletes a survey response and subsequently all prompt response 
	// references.
	private static final String SQL_DELETE_SURVEY_RESPONSE =
		"DELETE FROM survey_response " +
		"WHERE id = ?";
	
	private static SurveyResponseQueries instance;
	
	/**
	 * Creates this object.
	 * 
	 * @param dataSource The DataSource to use to query the database.
	 */
	private SurveyResponseQueries(DataSource dataSource) {
		super(dataSource);
		
		instance = this;
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses made in
	 * a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsFromCampaign(final String campaignId) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(SQL_GET_IDS_FOR_CAMPAIGN, new Object[] { campaignId }, new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_FOR_CAMPAIGN + 
						"' with parameter: " + 
							campaignId,
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses made by
	 * a given user in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param username The user's username.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsFromUser(final String campaignId, final String username) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(SQL_GET_IDS_FOR_USER, new Object[] { username, campaignId }, new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_FOR_USER + 
						"' with parameters: " + 
							username + ", " + 
							campaignId,
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses made by
	 * a given client in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param client The client value.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsWithClient(final String campaignId, final String client) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_WITH_CLIENT, 
					new Object[] { campaignId, client }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_FOR_USER + 
						"' with parameters: " + 
							campaignId + ", " + 
							client,
					e);
		}
	}

	/**
	 * Retrieves the survey response ID for all of the survey responses made on
	 * or after a given date in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param startDate The date.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsAfterDate(final String campaignId, final Date startDate) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_AFTER_DATE, 
					new Object[] { campaignId, TimeUtils.getIso8601DateString(startDate) }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_AFTER_DATE + 
						"' with parameters: " + 
							campaignId + ", " + 
							TimeUtils.getIso8601DateString(startDate),
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses made on
	 * or before a given date in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param endDate The date.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsBeforeDate(final String campaignId, final Date endDate) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_BEFORE_DATE, 
					new Object[] { campaignId, TimeUtils.getIso8601DateString(endDate) }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_BEFORE_DATE + 
						"' with parameters: " + 
							campaignId + ", " + 
							TimeUtils.getIso8601DateString(endDate),
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses with a
	 * given privacy state in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param privacyState The privacy state.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsWithPrivacyState(final String campaignId, 
			final SurveyResponse.PrivacyState privacyState) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_WITH_PRIVACY_STATE, 
					new Object[] { campaignId, privacyState.toString() }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_WITH_PRIVACY_STATE + 
						"' with parameters: " + 
							campaignId + ", " + 
							privacyState,
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses with a
	 * given survey ID in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param surveyId The survey's unique identifier.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsWithSurveyId(final String campaignId, final String surveyId) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_WITH_SURVEY_ID, 
					new Object[] { campaignId, surveyId }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +  
							SQL_GET_IDS_WITH_SURVEY_ID + 
						"' with parameters: " + 
							campaignId + ", " + 
							surveyId,
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses with a
	 * given prompt ID in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param promptId The prompt's unique identifier.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsWithPromptId(final String campaignId, final String promptId) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_WITH_PROMPT_ID, 
					new Object[] { campaignId, promptId }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_WITH_PROMPT_ID + 
						"' with parameters: " + 
							campaignId + ", " + 
							promptId,
					e);
		}
	}
	
	/**
	 * Retrieves the survey response ID for all of the survey responses with a
	 * given prompt type in a given campaign.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param promptType The prompt's type.
	 * 
	 * @return A, possibly empty but never null, list of survey response unique
	 * 		   identifiers.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<Long> retrieveSurveyResponseIdsWithPromptType(final String campaignId, final String promptType) throws DataAccessException {
		try {
			return instance.getJdbcTemplate().query(
					SQL_GET_IDS_WITH_PROMPT_TYPE, 
					new Object[] { campaignId, promptType }, 
					new SingleColumnRowMapper<Long>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + 
							SQL_GET_IDS_WITH_PROMPT_TYPE + 
						"' with parameters: " + 
							campaignId + ", " + 
							promptType,
					e);
		}
	}
	
	/**
	 * Retrieves the information about a survey response including all of the
	 * individual prompt responses.
	 * 
	 * @param campaign The campaign that contains all of the surveys.
	 * 
	 * @param surveyResponseId The survey response's unique identifier.
	 * 
	 * @return A SurveyResponseInformation object.
	 * 
	 * @throws DataAccessException Thrown if there is an error. This may be due
	 * 							   to a problem with the database or an issue
	 * 							   with the content in the database not being
	 * 							   understood by the SurveyResponseInformation
	 * 							   constructor.
	 */
	public static SurveyResponse retrieveSurveyResponseFromId(
			final Campaign campaign,
			final Long surveyResponseId) throws DataAccessException {
		
		try {
			// Create the survey response information object from the database.
			final SurveyResponse result = instance.getJdbcTemplate().queryForObject(
					SQL_GET_SURVEY_RESPONSE,
					new Object[] { surveyResponseId },
					new RowMapper<SurveyResponse>() {
						@Override
						public SurveyResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
							try {
								JSONObject locationJson = null;
								String locationString = rs.getString("location");
								if(locationString != null) {
									locationJson = new JSONObject(locationString);
								}
								
								return new SurveyResponse(
										campaign.getSurveys().get(rs.getString("survey_id")),
										rs.getLong("id"),
										rs.getString("username"),
										rs.getString("urn"),
										rs.getString("client"),
										rs.getTimestamp("msg_timestamp"),
										rs.getLong("epoch_millis"),
										TimeZone.getTimeZone(rs.getString("phone_timezone")),
										new JSONObject(rs.getString("launch_context")),
										rs.getString("location_status"),
										locationJson,
										SurveyResponse.PrivacyState.getValue(rs.getString("privacy_state")));
							}
							catch(JSONException e) {
								throw new SQLException("Error creating a JSONObject.", e);
							}
							catch(ErrorCodeException e) {
								throw new SQLException("Error creating the survey response information object.", e);
							}
							catch(IllegalArgumentException e) {
								throw new SQLException("Error creating the survey response information object.", e);
							}
						}
					}
				);
			
			final String surveyId = result.getSurvey().getId();
			
			final Map<String, Class<?>> typeMapping = new HashMap<String, Class<?>>();
			typeMapping.put("tinyint", Integer.class);
			
			// Retrieve all of the prompt responses for the survey response and
			// add them to the survey response information object.
			instance.getJdbcTemplate().query(
					SQL_GET_PROMPT_RESPONSES,
					new Object[] { surveyResponseId },
					new RowMapper<Object>() {
						@Override
						public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
							try {
								Prompt prompt = campaign.getPrompt(surveyId, rs.getString("prompt_id"));
								result.addPromptResponse(prompt.createResponse(rs.getString("response"), (Integer) rs.getObject("repeatable_set_iteration", typeMapping)));
							}
							catch(IllegalArgumentException e) {
								throw new SQLException("The prompt response value from the database is not a valid response value for this prompt.", e);
							}
							
							return null;
						}
					}
				);
			
			return result;
		}
		catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
			if(e.getActualSize() > 1) {
				throw new DataAccessException(
						"Multiple survey response's have the same database ID.", 
						e);
			}
			
			return null;
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + SQL_GET_SURVEY_RESPONSE + 
						"' with parameter: " + surveyResponseId,
					e);
		}
	}
	
	/**
	 * Retrieves the information about a list of survey responses including all
	 * of their individual prompt responses.
	 * 
	 * @param campaign The campaign that contains all of the surveys.
	 * 
	 * @param surveyResponseIds A collection of unique identifiers for survey
	 * 							responses whose information is being queried.
	 * 
	 * @return A list of SurveyResponseInformation objects each relating to a
	 * 		   survey response ID.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static List<SurveyResponse> retrieveSurveyResponseFromIds(
			final Campaign campaign,
			final Collection<Long> surveyResponseIds) throws DataAccessException {
		try {
			final Map<String, Class<?>> typeMapping = new HashMap<String, Class<?>>();
			typeMapping.put("tinyint", Integer.class);
			
			final List<SurveyResponse> result = 
				instance.getJdbcTemplate().query(
					SQL_GET_SURVEY_RESPONSES + StringUtils.generateStatementPList(surveyResponseIds.size()),
					surveyResponseIds.toArray(),
					new RowMapper<SurveyResponse>() {
						@Override
						public SurveyResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
							try {
								JSONObject locationJson = null;
								String locationString = rs.getString("location");
								if(locationString != null) {
									locationJson = new JSONObject(locationString);
								}
								
								// Create an object for this survey response.
								final SurveyResponse currResult = 
									new SurveyResponse(
										campaign.getSurveys().get(rs.getString("survey_id")),
										rs.getLong("id"),
										rs.getString("username"),
										rs.getString("urn"),
										rs.getString("client"),
										rs.getTimestamp("msg_timestamp"),
										rs.getLong("epoch_millis"),
										TimeZone.getTimeZone(rs.getString("phone_timezone")),
										new JSONObject(rs.getString("launch_context")),
										rs.getString("location_status"),
										locationJson,
										SurveyResponse.PrivacyState.getValue(rs.getString("privacy_state")));
								
								final String surveyId = currResult.getSurvey().getId();
								
								// Retrieve all of the prompt responses for the
								// current survey response.
								try {
									instance.getJdbcTemplate().query(
											SQL_GET_PROMPT_RESPONSES,
											new Object[] { rs.getLong("id") },
											new RowMapper<Object>() {
												@Override
												public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
													try {
														// Retrieve the prompt
														// from the 
														// configuration.
														Prompt prompt = 
															campaign.getPrompt(
																	surveyId,
																	rs.getString("prompt_id")
																);
														
														currResult.addPromptResponse(
																prompt.createResponse(
																		rs.getString("response"), 
																		(Integer) rs.getObject("repeatable_set_iteration", typeMapping)
																	)
															);
													}
													catch(IllegalArgumentException e) {
														throw new SQLException("The prompt response value from the database is not a valid response value for this prompt.", e);
													}
													
													return null;
												}
											}
										);
								}
								catch(org.springframework.dao.DataAccessException e) {
									throw new SQLException(
											"Error executing SQL '" + SQL_GET_PROMPT_RESPONSES + 
											"' with parameter: " + rs.getLong("id"),
										e);
								}
								
								return currResult;
							}
							catch(JSONException e) {
								throw new SQLException("Error creating a JSONObject.", e);
							}
							catch(ErrorCodeException e) {
								throw new SQLException("Error creating the survey response information object.", e);
							}
							catch(IllegalArgumentException e) {
								throw new SQLException("Error creating the survey response information object.", e);
							}
						}
					}
				);
			
			return result;
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" + SQL_GET_SURVEY_RESPONSE + 
						"' with parameter: " + surveyResponseIds,
					e);
		}
	}
	
	/**
	 * Updates the privacy state on a survey response.
	 * 
	 * @param surveyResponseId The survey response's unique identifier.
	 * @param privacyState The survey's new privacy state
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static void updateSurveyResponsePrivacyState(Long surveyResponseId, SurveyResponse.PrivacyState newPrivacyState) throws DataAccessException {
		// Create the transaction.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Updating a survey response.");
		
		try {
			// Begin the transaction.
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(instance.getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def);
			
			try {
				instance.getJdbcTemplate().update(
						SQL_UPDATE_SURVEY_RESPONSE_PRIVACY_STATE, 
						new Object[] { newPrivacyState.toString(), surveyResponseId });
			}
			catch(org.springframework.dao.DataAccessException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error executing SQL '" + SQL_UPDATE_SURVEY_RESPONSE_PRIVACY_STATE + 
					"' with parameters: " + newPrivacyState + ", " + surveyResponseId, e);
			}
			
			// Commit the transaction.
			try {
				transactionManager.commit(status);
			}
			catch(TransactionException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while committing the transaction.", e);
			}
		}
		catch(TransactionException e) {
			throw new DataAccessException("Error while attempting to rollback the transaction.", e);
		}
	}
	
	/**
	 * Deletes a survey response.
	 * 
	 * @param surveyResponseId The survey response's unique identifier.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public static void deleteSurveyResponse(Long surveyResponseId) throws DataAccessException {
		// Create the transaction.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Deleting a survey response.");
		
		try {
			// Begin the transaction.
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(instance.getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def);
			
			try {
				instance.getJdbcTemplate().update(
						SQL_DELETE_SURVEY_RESPONSE, 
						new Object[] { surveyResponseId });
			}
			catch(org.springframework.dao.DataAccessException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error executing SQL '" + SQL_DELETE_SURVEY_RESPONSE + "' with parameter: " + surveyResponseId, e);
			}
			
			// Commit the transaction.
			try {
				transactionManager.commit(status);
			}
			catch(TransactionException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while committing the transaction.", e);
			}
		}
		catch(TransactionException e) {
			throw new DataAccessException("Error while attempting to rollback the transaction.", e);
		}
	}
}