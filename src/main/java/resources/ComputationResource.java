package resources;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.MediaType;


import com.google.gson.Gson;
import util.AuthToken;
import util.ChangeAccStatusData;
import util.ChangeRoleData;
import Enum.Roles;
import Enum.States;
import util.ListUsersData;

@Path("/utils")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ComputationResource {

	private static final Logger LOG = Logger.getLogger(ComputationResource.class.getName());
	private final Gson g = new Gson();

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	public ComputationResource() {} //nothing to be done here @GET

	@POST
	@Path("/changeRole")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeRole(ChangeRoleData data, @Context HttpServletRequest request) {
		AuthToken token = (AuthToken) request.getAttribute("authToken");

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
	public Response changeAccountState(ChangeAccStatusData data, @Context HttpServletRequest request) {

		AuthToken token = (AuthToken) request.getAttribute("authToken");

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
			String targetStatus = targetUser.getString("account_status");

			if (targetStatus.equals(data.newState.toUpperCase())) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("The user already has this state").build();
			}
			if (canUpdateStatus(loggedInRole,targetStatus,data.newState.toUpperCase())) {
				Entity updatedUser = updateUserStatus(targetUser, data.newState.toUpperCase());
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
			States target = States.valueOf(targetStatus);
			States newS = States.valueOf(newState);
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

	@POST
	@Path("/listUsers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listUsers(@Context HttpServletRequest request) {

		AuthToken token = (AuthToken) request.getAttribute("authToken");

		String role = token.role;

		Query<Entity> query;

		if (Roles.ENDUSER.toString().equals(role)) {
			query = Query.newEntityQueryBuilder()
					.setKind("User")
					.setFilter(CompositeFilter.and(
							PropertyFilter.eq("user_role", "ENDUSER"),
							PropertyFilter.eq("user_privacy", "publico"),
							PropertyFilter.eq("account_status", "ATIVADA")
					))
					.build();
		} else if (Roles.BACKOFFICE.toString().equals(role)) {
			query = Query.newEntityQueryBuilder()
					.setKind("User")
					.setFilter(PropertyFilter.eq("user_role", "ENDUSER"))
					.build();
		} else if (Roles.ADMIN.toString().equals(role)) {
			query = Query.newEntityQueryBuilder()
					.setKind("User")
					.build();
		} else {
			return Response.status(Status.FORBIDDEN).entity("Not authorized to list users").build();
		}

		QueryResults<Entity> results = datastore.run(query);
		List<ListUsersData> usersList = new ArrayList<>();

		while (results.hasNext()) {
			Entity user = results.next();

			if (Roles.ENDUSER.toString().equals(role)) {
				usersList.add(new ListUsersData(user.getKey().getName(), user.getString("user_email"), user.getString("user_name")));
			} else {
				usersList.add(new ListUsersData(user));
			}
		}

		return Response.ok(g.toJson(usersList)).build();
	}
}