#!/usr/bin/groovy
node{

  checkout scm

  def pom = readMavenPom file: 'pom.xml'

  def githubOrganisation = 'fabric8io'
  def projectName = 'kubeflix'
  def dockerOrganisation = 'fabric8'
  def artifactIdToWatchInCentral = 'turbine-server'
  def artifactIdToWatchInCentralExtension = 'war'
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

    sh "git remote set-url origin git@github.com:${githubOrganisation}/${projectName}.git"

    def stagedProject = stageProject{
      project = githubOrganisation+"/"+projectName
    }

    String pullRequestId = release {
      projectStagingDetails = stagedProject
      project = githubOrganisation+"/"+projectName
      helmPush = false
    }

    promoteImages{
      toRegistry = 'docker.io'
      org = dockerOrganisation
      project = projectName
      images = imagesToPromoteToDockerHub
      tag = stagedProject[1]
    }

    waitUntilPullRequestMerged{
      name = githubOrganisation+"/"+projectName
      prId = pullRequestId
    }

    // lets check for turbine-server jar to detect when sonartype -> central sync has happened
    waitUntilArtifactSyncedWithCentral {
      repo = 'http://central.maven.org/maven2/'
      groupId = pom.groupId
      artifactId = artifactIdToWatchInCentral
      version = stagedProject[1]
      ext = artifactIdToWatchInCentralExtension
    }
  }
}
