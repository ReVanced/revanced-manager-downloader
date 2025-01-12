<p align="center">
  <picture>
    <source
      width="256px"
      media="(prefers-color-scheme: dark)"
      srcset="assets/revanced-headline/revanced-headline-vertical-dark.svg"
    >
    <img 
      width="256px"
      src="assets/revanced-headline/revanced-headline-vertical-light.svg"
    >
  </picture>
  <br>
  <a href="https://revanced.app/">
     <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="assets/revanced-logo/revanced-logo.svg" />
         <img height="24px" src="assets/revanced-logo/revanced-logo.svg" />
     </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://github.com/ReVanced">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://i.ibb.co/dMMmCrW/Git-Hub-Mark.png" />
           <img height="24px" src="https://i.ibb.co/9wV3HGF/Git-Hub-Mark-Light.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="http://revanced.app/discord">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://reddit.com/r/revancedapp">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://t.me/app_revanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://x.com/revancedapp">
      <picture>
         <source media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/93124920/270180600-7c1b38bf-889b-4d68-bd5e-b9d86f91421a.png">
         <img height="24px" src="https://user-images.githubusercontent.com/93124920/270108715-d80743fa-b330-4809-b1e6-79fbdc60d09c.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://www.youtube.com/@ReVanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
     </picture>
   </a>
   <br>
   <br>
   Continuing the legacy of Vanced
</p>

# 👋🔌 ReVanced Manager downloader template

![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/ReVanced/revanced-manager-downloader-template/release.yml)
![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)

Template repository for ReVanced Manager downloader plugins.

## ❓ About

This is a template to create a new ReVanced Manager downloader repository. An example implementation is included.

##  🚀 Get started

To start using this template, follow these steps:

1. [Create a new repository using this template](https://github.com/new?template_name=revanced-manager-downloader-template&template_owner=ReVanced)
2. Set up the [build.gradle.kts](build.gradle.kts) file (Specifically, the [package nme](build.gradle.kts#L21).
3. Update dependencies in the [libs.versions.toml](gradle/libs.versions.toml) file
4. [Create a pass-phrased GPG master key and subkey](https://mikeross.xyz/create-gpg-key-pair-with-subkeys/)
   1. Add the private key as a secret named [GPG_PRIVATE_KEY](.github/workflows/release.yml#L51) to your repository
   2. Add the passphrase as a secret named [GPG_PASSPHRASE](.github/workflows/release.yml#L52) to your repository
   3. Add the fingerprint of the GPG subkey as a secret named [GPG_FINGERPRINT](.github/workflows/release.yml#L53)
   to your repository
5. [Generate a keystore](https://developer.android.com/studio/publish/app-signing#generate-key)
   1. Add the Base64 encoded key store as a secret named [KEYSTORE](.github/workflows/release.yml#L57) to your repository
   2. Add the keystore password as a secret named [KEYSTORE_PASSWORD](.github/workflows/release.yml#L62) to your repository
   3. Add the keystore entry alias as a secret named [KEYSTORE_ENTRY_ALIAS](.github/workflows/release.yml#L63) to your repository
   4. Add the keystore entry password as a secret named [KEYSTORE_ENTRY_PASSWORD](.github/workflows/release.yml#L64) to your repository
7. Set up the [README.md](README.md) file[^1] (e.g, title, description, license, summary),
the [issue templates](.github/ISSUE_TEMPLATE)[^2] and the [contribution guidelines](CONTRIBUTING.md)[^3]

🎉 You are now ready to develop and release a ReVanced Manager downloader!

[^1]: [Example README.md file](https://github.com/ReVanced/revanced-manager/blob/main/README.md)
[^2]: [Example issue templates](https://github.com/ReVanced/revanced-manager/tree/main/.github/ISSUE_TEMPLATE)
[^3]: [Example contribution guidelines](https://github.com/ReVanced/revanced-manager/blob/main/CONTRIBUTING.md)

## 🧑‍💻 Usage

To develop and release ReVanced Manager downloader using this template, some things need to be considered:

- Development starts in feature branches. Once a feature branch is ready, it is squashed and merged into the `dev` branch
- The `dev` branch is merged into the `main` branch once it is ready for release
- Semantic versioning is used to version ReVanced Manager downloader.
- Semantic commit messages are used for commits
- Commits on the `dev` branch and `main` branch are automatically released
via the [release.yml](.github/workflows/release.yml) workflow, which is also responsible for generating the changelog
and updating the version of ReVanced Manager downloader. It is triggered by pushing to the `dev` or `main` branch.
The workflow uses the `publish` task to publish the release.
- The `publish` task depends on the `assembleRelease` task, so it will be run automatically when publishing a release.

## 📚 Everything else

### 📙 Contributing

Thank you for considering contributing to ReVanced Manager downloader.  
You can find the contribution guidelines [here](CONTRIBUTING.md).

### 🛠️ Building

To build ReVanced Manager downloader template, a Java Development Kit (JDK) and Git must be installed.  
Follow the steps below to build ReVanced Manager downloader template:

1. Run `git clone git@github.com:ReVanced/revanced-manager-downloader-template.git` to clone the repository
2. Run `gradlew assembleRelease` to build the project

> [!NOTE]
> If the build fails due to authentication, you may need to authenticate to GitHub Packages.
> Create a PAT with the scope `read:packages` [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced) and add your token to ~/.gradle/gradle.properties.
>
> Example `gradle.properties` file:
>
> ```properties
> gpr.user = user
> gpr.key = key
> ```

## 📜 Licence

ReVanced Manager downloader template is licensed under the GPLv3 licence.
Please see the [license file](LICENSE) for more information.
[tl;dr](https://www.tldrlegal.com/license/gnu-general-public-license-v3-gpl-3) you may copy, distribute
and modify ReVanced Manager downloader template as long as you track changes/dates in source files.
Any modifications to ReVanced Manager downloader template must also be made available under the GPL,
along with build & install instructions.
