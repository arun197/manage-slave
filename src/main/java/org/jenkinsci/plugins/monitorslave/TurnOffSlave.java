package org.jenkinsci.plugins.monitorslave;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import hudson.model.*;
import hudson.node_monitors.*;
import hudson.slaves.*;
import hudson.slaves.OfflineCause.UserCause;

import java.util.concurrent.*;

import org.jenkinsci.plugins.buildstep.ManageSlaveBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import javax.servlet.ServletException;

import java.io.IOException;

import jenkins.model.Jenkins;

/**
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link TurnOffSlaveDescriptor#newInstance(StaplerRequest)} is invoked
 * and a new {@link TurnOffSlave} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #slaveName})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Arun Suresh
 */
public class TurnOffSlave extends ManageSlaveBuildStep {

    private String slaveName;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TurnOffSlave(String slaveName) {
        this.slaveName = slaveName;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getSlaveName() {
        return slaveName;
    }
    
    public void setSlaveName(String slaveName){
    	this.slaveName=slaveName;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    	EnvVars env = build.getEnvironment(listener);
    	String expandedSlaveName = env.expand(slaveName);
    	Jenkins jenkins=Jenkins.getInstance();
    	
    	Computer computer = jenkins.getComputer(expandedSlaveName);
    	User user = jenkins.getMe();
		OfflineCause offlineCause = new UserCause(user, "VM is going to be deleted");
		listener.getLogger().println("Marking "+computer.getDisplayName()+" as temporarily offline");
		computer.setTemporarilyOffline(true, offlineCause);
		computer.disconnect(offlineCause);
    	return true;
    }
    

    /**
     * Descriptor for {@link TurnOffSlave}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class TurnOffSlaveDescriptor extends ManageSlaveBuildStepDescriptor {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public TurnOffSlaveDescriptor() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'slaveName'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckSlaveName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the Name of the slave");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return super.configure(req,formData);
        }       

		@Override
		public String getDisplayName() {
			return "Turn off slave";
		}

    }
}

