package filters;

import com.google.api.client.http.HttpHeaders;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import resources.ComputationResource;
import resources.UpdateResource;
import util.AuthToken;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Provider
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());
    private static final Gson g = new Gson();
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();


    @Context HttpServletRequest httpRequest;

    public AuthFilter(){}


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        //Não aplica filtro
        if ("login".equals(path) || "register".equals(path)) {
            LOG.info("AuthFilter: Path " + path);
            return; // Não aplica filtro para login/register
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid Authorization header").build());
            return;
        }

        try {
            String tokenStr = authHeader.substring("Bearer ".length());
            AuthToken token = g.fromJson(tokenStr, AuthToken.class);

            if (Timestamp.now().compareTo(token.validity.to) > 0) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Token expired").build());
                return;
            }

            Key revokedKey = datastore.newKeyFactory().setKind("RevokedToken")
                    .newKey(token.validity.verificador);
            if (datastore.get(revokedKey) != null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Token has been revoked").build());
            }

            httpRequest.setAttribute("authToken", token);
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token").build());
        }
    }
}

