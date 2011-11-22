package org.ohmage.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ohmage.domain.Clazz;
import org.ohmage.exception.DataAccessException;
import org.ohmage.query.impl.ClassQueries.UserAndClassRole;

/**
 * Interface to facilitate mocking concrete implementations for test cases. 
 * 
 * @author John Jenkins
 * @author Joshua Selsky
 */
public interface IClassQueries {

	/**
	 * Creates a new class.
	 * 
	 * @param classId The unique identifier for the class.
	 * 
	 * @param className The name of the class.
	 * 
	 * @param classDescription An optional description of the class. This may
	 * 						   be null.
	 * 
	 * @throws DataAccessException Thrown if there is an error executing any of
	 * 							   the SQL.
	 */
	void createClass(String classId, String className, String classDescription)
			throws DataAccessException;

	/**
	 * Queries the database to see if a class exists.
	 * 
	 * @param classId The ID of the class in question.
	 * 
	 * @return Whether or not the class exists.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	Boolean getClassExists(String classId) throws DataAccessException;
	
	/**
	 * Returns the unique ID for every class in the system.
	 * 
	 * @return The unique ID for every class in the system.
	 * 
	 * @throws DataAccessException Thrown if there was an error.
	 */
	List<String> getAllClassIds() throws DataAccessException;
	
	/**
	 * Queries the database for class IDs whose ID is similar to the given one.
	 * 
	 * @param partialId The partial class ID to match against all other class
	 * 					IDs.
	 * 
	 * @return A, possibly empty but never null, collection of valid class IDs
	 * 		   that contain the given one.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	List<String> getClassIdsFromPartialId(String partialId) 
			throws DataAccessException;
	
	/**
	 * Queries the database for class IDs whose name is similar to the given 
	 * one.
	 * 
	 * @param partialName The partial class name to match against all other 
	 * 					  class names.
	 * 
	 * @return A, possibly empty but never null, collection of valid class IDs
	 * 		   that contain the given one.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	List<String> getClassIdsFromPartialName(String partialName) 
			throws DataAccessException;
	
	/**
	 * Queries the database for class IDs whose description is similar to the 
	 * given one.
	 * 
	 * @param partialDescription The partial class description to match against 
	 * 							 all other classIDs.
	 * 
	 * @return A, possibly empty but never null, collection of valid class IDs
	 * 		   that contain the given one.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	List<String> getClassIdsFromPartialDescription(String partialDescription) 
			throws DataAccessException;

	/**
	 * Aggregates the information about a class as well as a list of users and
	 * their roles in the class for a list of classes.
	 * 
	 * @param classIds The list of class IDs whose information, users, and
	 * 				   users' roles are desired.
	 * 
	 * @param requester The username of the user who is making this request.
	 * 
	 * @return A List of ClassInformation objects correlating to the 
	 * 		   parameterized list of class IDs. This may be an empty list, but
	 * 		   it will never be null.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	List<Clazz> getClassesInformation(
			Collection<String> classIds) 
			throws DataAccessException;

	/**
	 * Retrieves a List of UserAndClassRole objects where each object is one of
	 * the users in the class and their role.
	 * 
	 * @param classId The unique identifier for a class.
	 * 
	 * @return A List of UserAndClassRole objects.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	List<UserAndClassRole> getUserRolePairs(String classId)
			throws DataAccessException;

	/**
	 * Updates a class' information and adds and removes users from the class
	 * all as requested.
	 * 
	 * @param classId The class identifier to use to lookup which class to 
	 * 				  update.
	 * 
	 * @param className The class' new name or null in which case the name will
	 * 					not be updated.
	 * 
	 * @param classDescription The class' new description or null in which case
	 * 						   the description will not be updated.
	 *  
	 * @param userAndRolesToAdd A list of users and respective roles to 
	 * 							associate with this class.
	 * 
	 * @param usersToRemove A list of users and respective roles to remove from
	 * 						this class.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */

	List<String> updateClass(String classId, String className,
			String classDescription, Map<String, Clazz.Role> userAndRolesToAdd,
			Collection<String> usersToRemove) throws DataAccessException;

	/**
	 * Deletes a class.
	 * 
	 * @param classId The unique identifier for the class to be deleted.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	void deleteClass(String classId) throws DataAccessException;

}