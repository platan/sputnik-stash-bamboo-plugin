# Sputnik for Bitbucket Server and Bamboo

This plugin integrates [Sputnik](https://github.com/TouK/sputnik) with [Atlassian Bamboo](https://www.atlassian.com/software/bamboo) and [Bitbucket Server](https://www.atlassian.com/software/bitbucket/server) (previously Stash).

## Installation
<!--
Plugin is available in [Atlassian Marketplace](https://marketplace.atlassian.com/plugins/com.github.platan.bamboo.sputnik.sputnik-bamboo-plugin/server/overview). You can install it [from the Marketplace](https://confluence.atlassian.com/display/UPM/Finding+new+add-ons) or [by file upload](https://confluence.atlassian.com/display/UPM/Installing+add-ons). 
 -->
You can install it [by file upload](https://confluence.atlassian.com/display/UPM/Installing+add-ons). Distribution packages will be available at [releases page](https://github.com/platan/sputnik-stash-bamboo-plugin/releases).
This plugin requires Sputnik to be available at Bamboo agents. The most convenient way of using this plugin is to install Sputnik at Bamboo agent and define Sputnik capability. Alternatively, Sputnik path can be defined in task.

### How to define Sputnik capability
1. Download latest [Sputnik distribution](https://github.com/TouK/sputnik/releases) and install it on Bamboo agents.
2. Add Sputnik executable
  - If `sputnik` command is added to PATH go to `Bamboo administration` -> `Server capabilities`, then click `Detect server capabilities`. Sputnik should appear in `Executable` list.
  - Alternatively, you can add Sputnik capability by hand. Go to `Bamboo administration` -> `Server capabilities` -> `Add capability` and set:
    - `Capability type` to `Executable`
    - `Type` to `Sputnik`
    - `Executable label` - `A label to uniquely identify this executable`
    - `Path` - path to your sputnik executable

### How to define Sputnik path
If you don't want define Sputnik Capability you can alternatively set Sputnik path in task using `Sputnik path` field (Sputnik can be installed on Bamboo agent or can be downloaded and extracted before Sputnik Task).

## Configuration
Options listed below can be defined by Bamboo variables and as a task configuration:
- `connector.username`
- `connector.password`
- `connector.useHttps`
- `connector.verifySsl`
In order to define a particular option as a variable add a `sputnik.` prefix to original key and use it as a variable name. For example, use `sputnik.connector.username` instead of `connector.username` to set a username.

For security reasons `connector.useHttps` and `connector.verifySsl` by default are set to `true` and can be defined by global variables only. This setting can be changed. Simply add an extra global variable with a key composed of a original key you want to be defined by non global variable and a suffix `.override` with value `true`. For example, set `sputnik.connector.useHttps.override` to `true` if you want to let define `connector.useHttps` by non global variable or in task configuration.

Options listed below cannot be defined by the user. They are automatically set based on repository definition.
- `connector.host`
- `connector.port`
- `connector.path`
- `connector.project`
- `connector.repository`

`connector.type` is always set to `stash`.

Other [Sputnik options](https://github.com/TouK/sputnik/blob/master/src/main/java/pl/touk/sputnik/configuration/GeneralOption.java) can be defined in task `Configuration` field. 

## Usage
Requirements:
- Sputnik-Stash Bamboo plugin is installed in Bamboo
- Sputnik executable is available
- There is a bamboo plan which checks out project code from Bitbucket Server
- There is a pull request for a given branch

In order to run Sputnik you have to:

1. Add Sputnik task to plan
1. Set Sputnik configuration in `Configuration` field. 
1. Set Bitbucket username and Bitbucket password by plan variables (`sputnik.connector.username` and `sputnik.connector.password` respectively), if they are not defined globally. 

Under the hood Sputnik task will create a temporary file with configuration in a working directory. This file will be removed after task execution.  

## FAQ
> I'm getting error: "com.atlassian.bamboo.task.TaskException: Neither Sputnik capability nor Sputnik path is defined!"

Add Sputnik executable or define Sputnik path in task configuration.


> "com.atlassian.bamboo.task.TaskException: No pull request found for branch 'branch1'!"

There is no pull request for branch being checked out. Please create a pull request for this branch in Bitbucket Server.


> "com.atlassian.bamboo.task.TaskException: Variable connector.username is missing!"

Define global variable or plan variable with key `sputnik.connector.username`.  


> "com.github.platan.bamboo.sputnik.StashException: Stash REST API returned status code 401 for http://stash.mycompany.com/stash/rest/api/1.0/projects/PROJECT1/repos/repo1/pull-requests"

Please configure correct username and password for Bitbucket Server.


> "Exception in thread "main" pl.touk.sputnik.connector.stash.StashException: Error when listing files

> "Caused by: javax.net.ssl.SSLException: Unrecognized SSL message, plaintext connection?"

Please check your SSL connection. If you want to turn it off use `sputnik.connector.useHttps` global variable. 

## Development

Here are the SDK commands you'll use immediately:

- atlas-run   -- installs this plugin into the product and starts it on localhost
- atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
- atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
    - 'pi' reinstalls the plugin into the running product instance
- atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK

## Changelog
<!--
### 0.1.0 (2016-11-XX[TODO])
- initial release
-->
## License
This project is licenced under the [Apache License 2.0](https://github.com/platan/sputnik-stash-bamboo-plugin/releases).
