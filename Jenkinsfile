#!/usr/bin/groovy
node{

  checkout scm

  def pom = readMavenPom file: 'pom.xml'

  def githubOrganisation = 'fabric8io'
  def dockerOrganisation = 'fabric8'
  def artifactIdToWatchInCentral = 'turbine-server'
  def imagesToPromoteToDockerHub = ['hystrix-dashboard','turbine-server']

  kubernetes.pod('buildpod').withImage('fabric8/maven-builder:1.0')
  .withPrivileged(true)
  .withHostPathMount('/var/run/docker.sock','/var/run/docker.sock')
  .withEnvVar('DOCKER_CONFIG','/root/.docker/')
  .withSecret('jenkins-maven-settings','/root/.m2')
  .withSecret('jenkins-ssh-config','/root/.ssh')
  .withSecret('jenkins-git-ssh','/root/.ssh-git')
  .withSecret('jenkins-release-gpg','/root/.gnupg')
  .withSecret('jenkins-docker-cfg','/root/.docker')
  .inside {

    sh 'chmod 600 /root/.ssh-git/ssh-key'
    sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
    sh 'chmod 700 /root/.ssh-git'
    sh 'chmod 600 /root/.gnupg/pubring.gpg'
    sh 'chmod 600 /root/.gnupg/secring.gpg'
    sh 'chmod 600 /root/.gnupg/trustdb.gpg'
    sh 'chmod 700 /root/.gnupg'

    sh "git remote set-url origin git@github.com:${githubOrganisation}/${pom.artifactId}.git"

    def stagedProject = stageProject{
      project = githubOrganisation+"/"+pom.artifactId
    }

    String pullRequestId = release {
      projectStagingDetails = stagedProject
      project = githubOrganisation+"/"+pom.artifactId
      helmPush = false
    }

    promoteImages{
      toRegistry = 'docker.io'
      org = dockerOrganisation
      project = pom.artifactId
      images = []
      tag = stagedProject[1]
    }

    waitUntilPullRequestMerged{
      name = githubOrganisation+"/"+pom.artifactId
      prId = pullRequestId
    }

    // lets check for turbine-server jar to detect when sonartype -> central sync has happened
    waitUntilArtifactSyncedWithCentral {
      repo = 'http://central.maven.org/maven2/'
      groupId = pom.groupId
      artifactId = artifactIdToWatchInCentral
      version = stagedProject[1]
      ext = 'jar'
    }
  }
}
