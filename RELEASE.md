## Building

The production build and deploy of this application is done using the fly CLI.

- `fly auth login` to select the account to which deployment will take place
- Go to the github repository and check what the new version number should be
- edit `release.sh` to set the VERSION variable and confirm the application name
  - if `--local-only` is present, start local Docker
  - for a remote build, remove `--local-only`

- run `release.sh`
  - fly will notice the presence of a Dockerfile 
  - fly will use the Docker file to build the application on a remote docker server
  - fly will deploy the application under the name indicated in release.sh
  - gh will create a release with the tag and ReleaseNotes.md

For development, the application is like all the other owlcms apps, but must run on Linux
  - start a wsl shell, cd to the git directory
  - run `code .` to start VSCode (at the bottom left you should see it is running on WSL)
  - Switch to a Java profile in the gear setting at the bottom
  - You can then start fly-owlcms from the run menu (left pointing triangle in the left hand-side bar)
  
  

## Getting the container

To pull an image from `registry.fly.io`, you need to authenticate with the Fly.io registry and then use the Docker CLI to pull the image. Here are the steps:

#### Step 1: Authenticate with Fly.io Registry
1. **Open your terminal**.
2. **Run the following command** to authenticate with the Fly.io registry:
   ```bash
   fly auth docker
   ```
   This command adds `registry.fly.io` to the Docker daemon's authenticated registries².

#### Step 2: Pull the Image
1. **Identify the image name** you want to pull. You can find the image name by running:
   ```bash
   fly releases --image
   ```
   This command will return a table with a column called 'Docker Image', which will look something like `registry.fly.io/app-name-here@sha256:long-hash-here`³.

2. **Pull the image** using the Docker CLI:
   ```bash
   docker pull registry.fly.io/app-name-here@sha256:long-hash-here
   ```

This will pull the specified image from the Fly.io registry to your local machine.

