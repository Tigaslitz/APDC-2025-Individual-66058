package resources;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.tasks.v2.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.HttpHeaders;


import com.google.cloud.tasks.v2.*;
import com.google.gson.Gson;
import com.google.protobuf.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import org.apache.commons.logging.Log;
import util.AuthToken;
import util.ChangeAccStatusData;
import util.ChangeRoleData;
import Enum.Roles;
import Enum.States;

@Path("/utils")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
public class ComputationResource {

	private static final Logger LOG = Logger.getLogger(ComputationResource.class.getName()); 
	private final Gson g = new Gson();

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	public ComputationResource() {} //nothing to be done here @GET

	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	public Response hello() throws IOException{
		try {
			throw new IOException("UPS");
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception on Method /hello", e);
			return Response.temporaryRedirect(URI.create("/error/500.html")).build();
		}
	}
	
	@GET
	@Path("/time")
	public Response getCurrentTime() {

		LOG.fine("Replying to date request.");
		return Response.ok().entity(g.toJson(fmt.format(new Date()))).build();
	}
	
	@GET
	@Path("/compute")
	public Response triggerExecuteComputeTask() throws IOException {
		String projectId = "jedi-master-v5-453321";
		String queueName = "Default";
		String location = "europe-west6";
		LOG.log(Level.INFO, projectId + " :: " + queueName + " :: " + location );

		try (CloudTasksClient client = CloudTasksClient.create()) {
			String queuePath = QueueName.of(projectId, location, queueName).toString();
			Task.Builder taskBuilder = Task.newBuilder().setAppEngineHttpRequest(AppEngineHttpRequest.newBuilder()
							.setRelativeUri("/rest/utils/compute").setHttpMethod(HttpMethod.POST).build());

			taskBuilder.setScheduleTime(Timestamp.newBuilder().setSeconds(Instant.now(Clock.systemUTC()).getEpochSecond()));
			
			client.createTask(queuePath, taskBuilder.build());
		} 
		return Response.ok().build();
	}
	
	@POST
	@Path("/compute")
	public Response executeComputeTask() {
		LOG.fine("Starting to execute computation tasks");
		try {
			Thread.sleep(60*1000*10); //10 min...
		} catch(Exception e) {
			LOG.logp(Level.SEVERE,  this.getClass().getCanonicalName(), "executeComputeTask", "An exception has occured");
			return Response.serverError().build();
		} //Simulates 60s execution
		return Response.ok().build();
	}

	@POST
	@Path("/changeRole")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeRole(ChangeRoleData data, @Context HttpServletRequest request,@Context HttpHeaders headers) {

		String authHeader = headers.getHeaderString("Authorization");
		String tokenStr = authHeader.substring("Bearer ".length());
		AuthToken token = g.fromJson(tokenStr, AuthToken.class);
		String loggedInUsername = token.username;

		Key loggedInKey = datastore.newKeyFactory().setKind("User").newKey(loggedInUsername);
		Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.target);

		Transaction txn = datastore.newTransaction();

		try {
			Entity loggedInUser = txn.get(loggedInKey);
			Entity targetUser = txn.get(targetUserKey);

			if (loggedInUser == null) {
				txn.rollback();
				return Response.status(Status.UNAUTHORIZED).entity("User not found").build();
			}
			if (targetUser == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("Target user not found").build();
			}

			String loggedInRole =  loggedInUser.getString("user_role");
			String targetRole = targetUser.getString("user_role");

			if (targetRole.equals(data.newRole.toUpperCase())) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("The user already has this role").build();
			}
			if (canUpdateRole(loggedInRole,targetRole,data.newRole.toUpperCase())) {
				Entity updatedUser = updateUserRole(targetUser, data.newRole);
				txn.put(updatedUser);
				txn.commit();
				return Response.ok().entity("Role updated successfully").build();
			}else {
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("You are not authorized to change this role").build();
			}
		} catch (Exception e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating role").build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	/**
	 * Private method to update a user role
	 * @param targetUser - the target user
	 * @param newRole - the role that is being assigned
	 * @return the entity corresponding to the given key
	 */
	private Entity updateUserRole(Entity targetUser, String newRole){
		Entity.Builder builder = Entity.newBuilder(targetUser);
		builder.set("user_role", newRole);
		return builder.build();
	}

	/**
	 * Checks if it is possible to update the target's role
	 * @param loggedInRole - role of the logged client
	 * @param targetRole - role of the client we want to update
	 * @param newRole - the new role
	 * @return if the update is possible
	 */
	private boolean canUpdateRole(String loggedInRole, String targetRole, String newRole){
		try {
			Roles logged = Roles.valueOf(loggedInRole.toUpperCase());
			Roles target = Roles.valueOf(targetRole.toUpperCase());
			Roles newR = Roles.valueOf(newRole.toUpperCase());
			// BACKOFFICE
			if (logged == Roles.BACKOFFICE) {
				return (target == Roles.ENDUSER || target == Roles.PARTNER)
						&& (newR == Roles.ENDUSER || newR == Roles.PARTNER);
			}
			//ADMIN
			return logged == Roles.ADMIN;
		} catch (IllegalArgumentException e) {
			return false;
		}
    }


	@POST
	@Path("/changeStatus")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeAccountState(ChangeAccStatusData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

		String authHeader = headers.getHeaderString("Authorization");
		String tokenStr = authHeader.substring("Bearer ".length());
		AuthToken token = g.fromJson(tokenStr, AuthToken.class);
		String loggedInUsername = token.username;

		LOG.info("merda "+loggedInUsername);
		Key loggedInKey = datastore.newKeyFactory().setKind("User").newKey(loggedInUsername);
		LOG.info("merda "+loggedInKey);
		Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.target);
		LOG.info("merda "+targetUserKey);

		Transaction txn = datastore.newTransaction();

		try {
			Entity loggedInUser = txn.get(loggedInKey);
			Entity targetUser = txn.get(targetUserKey);
			LOG.info("merda "+loggedInUser + " " + targetUser );

			if (loggedInUser == null) {
				txn.rollback();
				return Response.status(Status.UNAUTHORIZED).entity("User not found").build();
			}
			if (targetUser == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("Target user not found").build();
			}

			String loggedInRole =  loggedInUser.getString("user_role");
			LOG.info("alibaba " + loggedInRole);
			String targetStatus = targetUser.getString("account_status");
			LOG.info("aqui "+ targetStatus);

			if (targetStatus.equals(data.newState.toUpperCase())) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("The user already has this state").build();
			}
			if (canUpdateStatus(loggedInRole,targetStatus,data.newState.toUpperCase())) {
				Entity updatedUser = updateUserStatus(targetUser, data.newState);
				txn.put(updatedUser);
				txn.commit();
				return Response.ok().entity("Status updated successfully").build();
			}else {
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("You are not authorized to change this state").build();
			}
		} catch (Exception e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating state").build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	/**
	 * Private method to update a user role
	 * @param targetUser- the target user
	 * @param newState - the status that is being assigned
	 * @return the entity corresponding to the given key
	 */
	private Entity updateUserStatus(Entity targetUser, String newState) {
		Entity.Builder builder = Entity.newBuilder(targetUser);
		builder.set("account_status", newState);
		return builder.build();
	}

	/**
	 * Returns if the logged user has permission to update the target status
	 * @param loggedInRole - logged user role
	 * @param targetStatus - current target status
	 * @param newState - new state for the target user
	 */
	private boolean canUpdateStatus(String loggedInRole, String targetStatus, String newState ){
		try {
			Roles logged = Roles.valueOf(loggedInRole);
			LOG.info("pilao " + logged);
			States target = States.valueOf(targetStatus);
			LOG.info("pilar " + logged);
			States newS = States.valueOf(newState);
			LOG.info("pilas " + logged);
			// BACKOFFICE
			if (logged == Roles.BACKOFFICE) {
				return (target == States.ATIVADA || target == States.DESATIVADA)
						&& (newS == States.ATIVADA || newS == States.DESATIVADA);
			}
			//ADMIN
			return logged == Roles.ADMIN;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}