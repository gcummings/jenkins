/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Tom Huybrechts, Geoff Cummings
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import jenkins.model.Jenkins;
import hudson.model.Result;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import hudson.Extension;
import hudson.util.RunList;

import org.kohsuke.stapler.QueryParameter;

public class RunParameterDefinition extends SimpleParameterDefinition {

    private final String projectName;
    private final String runId;
    private final String threshold;
    private static final String ALL = "ALL";

    @DataBoundConstructor
    public RunParameterDefinition(String name, String projectName, String description, String threshold) {
        super(name, description);
        this.projectName = projectName;
        this.runId = null;
        this.threshold = threshold;
    }

    private RunParameterDefinition(String name, String projectName, String runId, String description, String threshold) {
        super(name, description);
        this.projectName = projectName;
        this.runId = runId;
        this.threshold = threshold;
    }

    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof RunParameterValue) {
            RunParameterValue value = (RunParameterValue) defaultValue;
            return new RunParameterDefinition(getName(), value.getRunId(), getDescription(), getThreshold());
        } else {
            return this;
        }
    }

    private boolean isAllBuilds() {
        return (null == threshold || ALL.equals(threshold));
    }

    @Exported
    public String getProjectName() {
        return projectName;
    }

    public Job getProject() {
        return Jenkins.getInstance().getItemByFullName(projectName, Job.class);
    }

    /**
     * @since 1.508
     * @return Returns a list of builds which meet the parameters configured threshold
     */
    public RunList getBuilds() {
        if (isAllBuilds()) {
            return getProject().getBuilds();
        }
        return getProject().getBuilds().overThresholdOnly(getThresholdResult());
    }

    public String getThreshold() {
        return threshold;
    }

    private Result getThresholdResult() {
        return Result.fromString(threshold);
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.RunParameterDefinition_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/help/parameter/run.html";
        }

        @Override
        public ParameterDefinition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(RunParameterDefinition.class, formData);
        }
        
        public AutoCompletionCandidates doAutoCompleteProjectName(@QueryParameter String value) {
            return AutoCompletionCandidates.ofJobNames(Job.class, value, null, Jenkins.getInstance());
        }

    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        if (runId != null) {
            return createValue(runId);
        }

        Run<?,?> lastBuild = null;
        if(isAllBuilds()) {
            lastBuild = getProject().getLastBuild();
        } else {
            lastBuild = getProject().getLastBuildOverThreshold(getThresholdResult());
        }

        if (lastBuild != null) {
            return createValue(lastBuild.getExternalizableId());
        } else {
            return null;
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        RunParameterValue value = req.bindJSON(RunParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    public RunParameterValue createValue(String value) {
        return new RunParameterValue(getName(), value, getDescription());
    }

}
