package com.redhat.jboss.maven.elprofile;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component(role = ProfileSelector.class, hint = "default")
public class ElProfileSelector extends DefaultProfileSelector {

    @Requirement
    private Logger logger;

    private int lol;
    public ElProfileSelector() {
        lol = 1;
    }

    //disable PropertyProfileActivator
    @Override
    public DefaultProfileSelector addProfileActivator(ProfileActivator profileActivator) {
        if (profileActivator != null && !(profileActivator instanceof PropertyProfileActivator)) {
            return super.addProfileActivator(profileActivator);
        }
        return this;
    }

    @Override
    public List<Profile> getActiveProfiles(Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {
        checkActivatorsList();
        return super.getActiveProfiles(profiles, context, problems);
    }

    private void checkActivatorsList() {
        try {
            Field activatorsField = getClass().getSuperclass().getDeclaredField("activators");
            activatorsField.setAccessible(true);
            List<ProfileActivator> activators = (List<ProfileActivator>) activatorsField.get(this);
            List<ProfileActivator> listWithoutPropertyActivator = activators.stream()
                    .filter(p -> !(p instanceof PropertyProfileActivator))
                    .collect(Collectors.toList());
            activatorsField.set(this, listWithoutPropertyActivator);
        } catch (IllegalAccessException e) {
            logger.warn("problem on change activators list", e);
        } catch (NoSuchFieldException e) {
            logger.warn("cant locate activators field", e);
        }

    }
}
