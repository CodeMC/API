FROM jenkins/jenkins:lts

COPY --chown=jenkins:jenkins init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/

# Maven
USER root
RUN apt-get update && apt-get install -y maven
USER jenkins

# Configuration
COPY --chown=jenkins:jenkins config.yml /usr/share/jenkins/ref/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/jenkins.yaml

# Plugins
COPY --chown=jenkins:jenkins plugins.yml /usr/share/jenkins/ref/plugins.yml
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.yml

# Skip Setup
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"