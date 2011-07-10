package com.redhat.openshift.resteasy;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

public interface RedHatSSODao {
	@POST
	@Path("/wapps/streamline/login.html")
	@Consumes("*/*")
	Response login(@FormParam("login") String login,@FormParam("password") String password);
}
