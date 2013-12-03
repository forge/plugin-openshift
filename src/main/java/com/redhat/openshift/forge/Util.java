package com.redhat.openshift.forge;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Alternative;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.PipeOut;

import com.openshift.client.ApplicationScale;
import com.openshift.client.HttpMethod;
import com.openshift.client.IApplication;
import com.openshift.client.IHttpClient;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.InvalidCredentialsOpenShiftException;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftEndpointException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.internal.client.IRestService;
import com.openshift.internal.client.RestService;
import com.openshift.internal.client.httpclient.UrlConnectionHttpClientBuilder;
import com.openshift.internal.client.httpclient.request.JsonMediaType;
import com.openshift.internal.client.httpclient.request.Parameter;
import com.openshift.internal.client.response.Link;
import com.openshift.internal.client.response.OpenShiftJsonDTOFactory;
import com.openshift.internal.client.response.RestResponse;

public class Util {

    private static final String GIT_URI_PATTERN = "ssh://{0}@{1}-{2}.{3}/~/git/{1}.git/";
    private static final String APPLICATION_URL_PATTERN = "https://{0}-{1}.{2}/";

    public static boolean isOpenshiftRemotePresent(ShellPrintWriter out, Project project) throws IOException {
        StoredConfig config = Git.open(project.getProjectRoot().getUnderlyingResourceObject()).getRepository().getConfig();
        Set<String> subsections = config.getSubsections("remote");
        for (String subsection : subsections) {
            String value = config.getString("remote", subsection, "url");
            if (value.contains("rhcloud.com")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGitInit(Project project) throws IOException {
        return project.getProjectRoot().getChildDirectory(".git").exists();
    }

    @Alternative
    @SuppressWarnings("unused")
    private static class DummyOut implements ShellPrintWriter {

        public DummyOut() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void write(byte b) {
            // TODO Auto-generated method stub

        }

        @Override
        public void print(String output) {
            // TODO Auto-generated method stub

        }

        @Override
        public void println(String output) {
            // TODO Auto-generated method stub

        }

        @Override
        public void println() {
            // TODO Auto-generated method stub

        }

        @Override
        public void print(ShellColor color, String output) {
            // TODO Auto-generated method stub

        }

        @Override
        public void println(ShellColor color, String output) {
            // TODO Auto-generated method stub

        }

        @Override
        public String renderColor(ShellColor color, String output) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void write(int b) {
            // TODO Auto-generated method stub
        }

        @Override
        public void write(byte[] b) {
            // TODO Auto-generated method stub
        }

        @Override
        public void write(byte[] b, int offset, int length) {
            // TODO Auto-generated method stub
        }

        @Override
        public void flush() {
            // TODO Auto-generated method stub}
        }
    }

    public static String getDefaultRhLogin(ShellPrintWriter out, OpenShiftConfiguration configuration) {
        return Util.unquote(configuration.getRhcProperties().getProperty("default_rhlogin"));
    }

    public static String getDefaultBaseUrl(ShellPrintWriter out, OpenShiftConfiguration configuration) {
        String hostname = Util.unquote(configuration.getRhcProperties().getProperty("libra_server"));

        if (hostname == null || hostname.trim().length() == 0) {
            return null;
        } else {
            return "https://".concat(hostname);
        }
    }

    public static int getDefaultDNSTimeout(ShellPrintWriter out, OpenShiftConfiguration configuration) {
        String dnsTimeout = Util.unquote(configuration.getRhcProperties().getProperty("dns_timeout"));

        if (dnsTimeout == null || dnsTimeout.trim().length() == 0) {
            return 500;
        }

        return Integer.parseInt(dnsTimeout);
    }

    public static String getName(String name, OpenShiftConfiguration configuration, Project project, ShellPrompt prompt) {
        if (name == null) {
            name = configuration.getName();
            if (name == null) {
                String _default = project.getFacet(MetadataFacet.class).getProjectName();
                _default = _default.replaceAll("[\\W_]", "");
                _default = _default.substring(0, (_default.length() > 15 ? 15 : _default.length()));
                name = prompt.prompt("Enter the application name [" + _default + "] ", String.class, _default);

                configuration.setName(name);
            }
        }
        return name;
    }

    public static String getRhLogin(String rhLogin, OpenShiftConfiguration configuration, ShellPrintWriter out,
            ShellPrompt prompt) {
        if (rhLogin == null) {
            rhLogin = configuration.getRhLogin();
            if (rhLogin == null) {
                String _default = getDefaultRhLogin(out, configuration);
                if (_default == null) {
                    ShellMessages.info(out, "If you do not have a Red Hat login, visit http://openshift.com");
                }
                rhLogin = prompt.prompt("Enter your Red Hat Login [" + _default + "] ", String.class, _default);
                configuration.setRhLogin(rhLogin);
            }
        }
        return rhLogin;
    }

    public static String getPassword(ShellPrompt prompt) {
        return prompt.promptSecret("Enter your Red Hat Openshift password: ");
    }

    public static void displayNonExistentDomainError(ShellPrintWriter out, NotFoundOpenShiftException e) {
        ShellMessages
                .error(out,
                        "It looks like you haven't created an OpenShift namespace.\nPlease log in to https://openshift.redhat.com to set up your namespace and SSH keys before running this command.\n");
    }

    public static void displayCredentialsError(ShellPrintWriter out, InvalidCredentialsOpenShiftException e) {
        ShellMessages.error(out, "Invalid user credentials.  Please check your Red Hat login and password and try again.\n");
    }

    public static void printApplicationInfo(ShellPrintWriter out, IApplication app, String namespace, String domain)
            throws OpenShiftException {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("Framework", app.getCartridge().getName());
        attrs.put("Creation", app.getCreationTime().toString());
        attrs.put("UUID", app.getUUID());

        // TODO: client library should provide these URIs
        attrs.put("Git URL", MessageFormat.format(GIT_URI_PATTERN, app.getUUID(), app.getName(), namespace, domain));
        attrs.put("Public URL", MessageFormat.format(APPLICATION_URL_PATTERN, app.getName(), namespace, domain));

        attrs.put("Embedded", formatEmbeddedCartridges(app.getEmbeddedCartridges()));

        int longest = 0;
        for (String key : attrs.keySet()) {
            longest = Math.max(longest, key.length());
        }

        final StringBuilder str = new StringBuilder();
        str.append(String.format(app.getName()));
        for (String key : attrs.keySet()) {
            str.append(String.format("\n  %s %s", pad(key + ":", longest), attrs.get(key)));
        }
        str.append("\n");
        out.println(str.toString());
    }

    private static String formatEmbeddedCartridges(List<IEmbeddedCartridge> cartridges) {
        if (cartridges.size() == 0) {
            return "None";
        }

        StringBuilder carts = new StringBuilder();

        for (IEmbeddableCartridge info : cartridges) {
            if (carts.length() > 0) {
                carts.append(", ");
            }
            carts.append(info.getName());
        }

        return carts.toString();
    }

    private static String pad(String str, int len) {
        StringBuilder result = new StringBuilder(str);
        for (int i = 0; i < len - str.length() + 1; i++) {
            result.append(" ");
        }
        return result.toString();
    }

    // unwraps strings wrapped in double or single quotes
    // intentionally package-protected
    static String unquote(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }

        char a = str.charAt(0);
        char z = str.charAt(str.length() - 1);

        if (z == a && (z == '"' || z == '\'')) {
            return str.substring(1, str.length() - 1);
        } else {
            return str;
        }
    }

    public static IApplication createApplication(IOpenShiftConnection openshift, IStandaloneCartridge cartridge, IUser user, String name,
            boolean scaling, ShellPrintWriter out) throws OpenShiftException {
        // Attempt to create the application
        IApplication application = null;
        ApplicationScale applicationScale = scaling ? ApplicationScale.SCALE : ApplicationScale.NO_SCALE;
        if (applicationScale == ApplicationScale.SCALE) {
            int timeout = OpenShiftPlugin.CONNECT_TIMEOUT_MINUTES;
            ShellMessages.info(out, "Scaling Applications takes longer to create. This operation can take up to " + timeout
                    + " minutes. Please wait...");
            System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(1000 * 60 * timeout));
        }
        try {
            application = openshift.getUser().getDefaultDomain().createApplication(name, cartridge, applicationScale);
        } catch (OpenShiftEndpointException e) {
            ShellMessages.error(out, "OpenShift failed to create the application");
            ShellMessages.error(out, e.getMessage());
            if (e.getCause().getClass() != null)
                ShellMessages.error(out, e.getCause().getMessage());
            return null;
        }
        return application;
    }

    public static void addCartridgeToApplication(IOpenShiftConnection openshift, String appname,
            IEmbeddableCartridge embeddableCartridge, PipeOut out) {
        try {
            openshift.getUser().getDefaultDomain().getApplicationByName(appname).addEmbeddableCartridge(embeddableCartridge);
            ShellMessages.success(out, String.format("Cartdrige %s added to %s", embeddableCartridge, appname));
        } catch (OpenShiftEndpointException e) {
            ShellMessages.error(out, e.getMessage());
        }
    }

    /**
     * @param app
     * @param baseUrl
     * @param out
     * @throws OpenShiftException
     * @throws SocketTimeoutException
     */
    public static void healthCheck(IApplication app, String baseUrl, PipeOut out) {
        String healthCheckURL = app.getApplicationUrl();
        ShellMessages.info(out, "Checking the Health Check URL: " + healthCheckURL);
        IHttpClient httpClient = new UrlConnectionHttpClientBuilder().client();
        IRestService service = new RestService(baseUrl,
            null,
            new JsonMediaType(),
            IHttpClient.MEDIATYPE_APPLICATION_JSON,
            new OpenShiftJsonDTOFactory(),
            httpClient);
        try {
            Link healthCheckLink = new Link(healthCheckURL, HttpMethod.GET);
            RestResponse response = service.request(healthCheckLink, (Parameter[]) null);
            if (response != null){
                ShellMessages.success(out, "You application is up and running");
            }
        } catch (Exception e) {
            ShellMessages.error(out, e.getMessage());
        }finally{
            ShellMessages.info(out, "For more information, please check the logs with the [rhc tail] command.");
        }

    }
}
