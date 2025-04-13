package resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import util.AuthToken;
import util.RegisterData;

import java.util.logging.Logger;
import Enum.Roles;
import util.RemoveUserData;


@Path("/remove")
public class RemoveResource {
    private static final String MESSAGE_INVALID_TARGET = "Invalid target username";
    private static final String LOG_MESSAGE_REMOVE_ATTEMPT = "Trying to remove ";


    private static final Logger LOG = Logger.getLogger(RemoveResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");


    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeUser(RemoveUserData target, @Context HttpServletRequest request, @Context HttpHeaders headers) {
        LOG.fine("Attempt to remove user: " + target.target);

        String authHeader = headers.getHeaderString("Authorization");
        String tokenStr = authHeader.substring("Bearer ".length());
        AuthToken token = g.fromJson(tokenStr, AuthToken.class);
        String loggedInUsername = token.username;

        Key LoggedInUserKey = userKeyFactory.newKey(loggedInUsername);
        Key targetKey = userKeyFactory.newKey(target.target);

        Transaction txn = datastore.newTransaction();
        try {
            Entity targetUser = txn.get(targetKey);
            if (targetUser == null) {
                Query<Entity> emailQuery = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(StructuredQuery.PropertyFilter.eq("user_email", target.target))
                        .setLimit(1)
                        .build();
                QueryResults<Entity> results = datastore.run(emailQuery);
                if (results.hasNext()) {
                    targetUser = results.next();
                } else {
                    // Username does not exist
                    LOG.warning(LOG_MESSAGE_REMOVE_ATTEMPT + target.target);
                    return Response.status(Response.Status.NOT_FOUND).entity(MESSAGE_INVALID_TARGET).build();
                }
            }
            Entity loggedInUser = txn.get(LoggedInUserKey);

            if (loggedInUser == null) {
                txn.rollback();
                return Response.status(Response.Status.UNAUTHORIZED).entity("User not found").build();
            }

            String loggedInRole =  loggedInUser.getString("user_role");
            String targetRole = targetUser.getString("user_role");

            if (targetUser.getKey().equals(loggedInUser.getKey())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("You cannot delete your own account").build();
            }

            if (canDeleteUser(loggedInRole,targetRole)) {
                txn.delete(targetUser.getKey());
                txn.commit();
                return Response.ok().entity("User account removed successfully").build();
            }else {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to change this role").build();
            }
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating role").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }

    }

    private Boolean canDeleteUser(String loggedInRole, String targetRole){
        try {
            Roles logged = Roles.valueOf(loggedInRole.toUpperCase());
            Roles target = Roles.valueOf(targetRole.toUpperCase());
            // BACKOFFICE
            if (logged == Roles.BACKOFFICE) {
                return (target == Roles.ENDUSER || target == Roles.PARTNER);
            }
            //ADMIN
            return logged == Roles.ADMIN;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
