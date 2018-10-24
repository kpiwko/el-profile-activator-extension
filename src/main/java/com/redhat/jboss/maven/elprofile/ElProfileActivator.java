/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.jboss.maven.elprofile;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.mvel2.CompileException;
import org.mvel2.MVEL;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alternative implementation of PropertyActivator for Maven 3
 * 
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * 
 */
@Component(role = ProfileActivator.class, hint = "property")
public class ElProfileActivator implements ProfileActivator {

    private static final Pattern COMMA_PAT = Pattern.compile(",");
    private static final Pattern MVEL_SCRIPT_PROPERTY_NAME_PAT = Pattern.compile("^mvel(?:\\(([^\\)]*+)\\))?+$");

    @Requirement
    private Logger logger;

    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problemCollector) {

        Activation activation = profile.getActivation();

        boolean result = false;

        if (activation != null)
        {
            ActivationProperty property = activation.getProperty();

            if (property != null)
            {
                String name = property.getName();

                Matcher matcher = MVEL_SCRIPT_PROPERTY_NAME_PAT.matcher((name == null) ? "" : name);
                if (matcher.matches()) {
                    List<String> parameters = Collections.<String>emptyList();
                    String parametersString = matcher.group(1);
                    parametersString = (parametersString == null) ? "" : parametersString.trim();
                    if (!parametersString.isEmpty()) {
                        parameters = Arrays.asList(COMMA_PAT.split(parametersString, -1));
                    }
                    String value = property.getValue();
                    logger.debug("Evaluating following MVEL expression: " + value);
                    result = evaluateMvel(value, parameters, context, problemCollector);
                    logger.debug("Evaluated MVEL expression: " + value + " as " + result);
                }
            }
        }

        // call original implementation if mvel script was not valid/false
        return result ? true : new PropertyProfileActivator().isActive(profile, context, problemCollector);
    }

    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problemCollector) {
        return new PropertyProfileActivator().presentInConfig(profile, context, problemCollector);
    }

    private boolean evaluateMvel(String expression, List<String> parameters, ProfileActivationContext context, ModelProblemCollector problemCollector) {

        if (expression == null || expression.length() == 0) {
            return false;
        }

        String propertiesIdentifier = null;
        Iterator<String> parametersIterator = parameters.iterator();
        if (parametersIterator.hasNext()) {
            String identifier = parametersIterator.next();
            identifier = (identifier == null) ? "" : identifier.trim();
            if (!identifier.isEmpty()) {
                propertiesIdentifier = identifier;
            }
        }

        try {
            // "casting" to <String,Object> and including both user and system properties
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.putAll(context.getSystemProperties());
            properties.putAll(context.getUserProperties());
            Map<String, Object> externalVariables = properties;
            if (propertiesIdentifier != null) {
                externalVariables = new HashMap<String, Object>();
                externalVariables.putAll(properties);
                // including properties map as specified identifier
                externalVariables.put(propertiesIdentifier, properties);
            }

            return MVEL.evalToBoolean(expression, externalVariables);
        } catch (NullPointerException e) {
            logger.warn("Unable to evaluate mvel property value (\"" + expression + "\")");
            logger.debug(e.getMessage());
            return false;
        } catch (CompileException e) {
            logger.warn("Unable to evaluate mvel property value (\"" + expression + "\")");
            logger.debug(e.getMessage());
            return false;
        }
    }

}
