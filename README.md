# The ONE v1.4.1 (with Gradle Build)

<p align="center">The Opportunistic Network Environmentis a simulator designed to research and analyze delay-tolerant networks (DTN). With this tool, you can:</p>
<p align="center">
  🗺️ Generate mobility traces for nodes in a simulation <br>
  💬 Run message exchange simulations using various DTN routing protocols <br>
  📊 Visualize simulation results in real-time and analyze them after the simulation ends
</p>

---
## 📖 Introduction

This repository contains a version of The ONE Simulator with custom modifications, based on the work originally done by the developers at Aalto University and Technische Universität München, and further extended in the `the-one-pitt` repository by pj dillon.

For comprehensive documentation on the **original ONE Simulator**, its features, and how to use its configuration files, please refer to:

*   For introduction and releases, see [the ONE homepage at GitHub](https://akeranen.github.io/the-one/)
*   For instructions on how to get started, see [wiki page](https://github.com/akeranen/the-one/wiki)
*   The [the README](https://github.com/akeranen/the-one/wiki/README) has the lastest information.

This repository builds upon the **`the-one-pitt`** modifications. To understand the specific changes and additions made in that version (such as BubbleRap, DecisionEngineRouter, etc.), you can refer to:

*   **The `the-one-pitt` modification of the-one** [the-one-pitt home at Github](https://github.com/knightcode/the-one-pitt.git)

---
## 🚀 Quick Start (Using Gradle)

This project is configured to use **Gradle** as its build automation and project management system. The necessary Gradle configuration files (`build.gradle`, `settings.gradle`, `gradle.properties`) and the Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) are already included in this repository.

### ⚙️ Prerequisites

To build and run this project, you will need:

*   **Java Development Kit (JDK) version 17 or compatible:** Ensure JDK 17 is installed on your system and the `JAVA_HOME` environment variable is correctly set. Compatible distributions include OpenJDK or Oracle JDK 17.
*   **Operating System:** Linux or Windows.
*   **Command Line Interface:** Access to a terminal (Bash, Zsh, Command Prompt, PowerShell).

You **do not need** to install Gradle globally. The included **Gradle Wrapper** will automatically handle the Gradle version required (Gradle 8.13).

### 🛠️ Getting Started

These steps will prepare the project for building and running simulations.

1.  **Clone the Repository:** Get the code onto your local machine:
    ```bash
    git clone https://github.com/hendrowunga/the-one-pitt.git
    cd the-one-pitt
    ```

2.  **Verify Project Structure:**
    Ensure the following files and folders are present at the root level of the `the-one-pitt` directory:
   *   `build.gradle`
   *   `settings.gradle`
   *   `gradlew` (for Linux/macOS)
   *   `gradlew.bat` (for Windows)
   *   `gradle/` folder
   *   `.gradle/` folder (may appear after the first Gradle command)
   *   `src/` folder (contains source code)
   *   `lib/` folder (contains required JAR libraries)
   *   `data/` folder (may contain input data)
   *   ... other original project files ...

3.  **Grant Wrapper Execution Permission (Linux/macOS Only):**
    Ensure the `gradlew` script has executable permissions:
    ```sh
    chmod +x gradlew
    ```
    Windows users can skip this step as `gradlew.bat` does not require explicit execution permissions.

### 🔨 Building the Project

The project build is managed by Gradle using the configuration already provided in `build.gradle`. This file defines the source code locations, dependencies, and how to package the application.

To compile all source code, manage dependencies (from `lib/` and potentially other repositories), and assemble the application JAR file, use the `build` task via the Gradle Wrapper:

*   **On Linux:**
    ```sh
    ./gradlew build
    ```
*   **On Windows (Command Prompt or PowerShell):**
    ```cmd
    gradlew build
    ```

The first time you run a `./gradlew` or `gradlew.bat` command, Gradle Wrapper will download the specified Gradle version. This might take some time depending on your internet connection. Subsequent commands will be faster as Gradle will use the downloaded version.

If the build is successful, you will see a `BUILD SUCCESSFUL` message in the terminal. The compiled classes and the application JAR file will be located in the `build/` directory at the project root (specifically, the JAR is usually in `build/libs/the-one-pitt-1.0-SNAPSHOT.jar`).

*(Note: The `build.gradle` file is pre-configured. **You should not need to manually edit `build.gradle`** to build or run the project unless you have advanced requirements like adding new dependencies or modifying the build process itself. Unit tests are currently disabled in the configuration to ensure the main build succeeds despite potential issues in outdated test code.)*

### 🏃 Running Simulations

You have two primary ways to run simulations using this Gradle setup: directly with the `run` task, or by creating a complete distribution package that mimics the original ONE Simulator execution method (`one.sh`/`one.bat`).

#### Running Directly with the `gradlew run` Task

The `run` task will execute the main application class (`core.DTNSim`) directly through Gradle. This method is convenient for quick runs or debugging.

*   **Running with Default Configuration:**
    A default configuration file (typically `default_settings.txt`) and mode (GUI or batch) is configured as default arguments for the `run` task in the `build.gradle` file. To run with this default, simply execute the `run` task without any `--args`:
   *   **On Linux:**
       ```sh
       ./gradlew run
       ```
   *   **On Windows:**
       ```cmd
       gradlew run
       ```

*   **Running with Specific Configuration (Overriding Default):**
    To run a simulation with a different configuration file, specify batch mode parameters, or pass custom arguments (like the `-d` option for overriding settings if supported by the ONE version), use the `--args` option followed by the arguments you want to pass to the ONE Simulator application. Arguments provided after `--args` will override the default arguments set for the `run` task in `build.gradle`.

   *   **Example 1: Run GUI with a different config file**
      *   On Linux:
          ```sh
          ./gradlew run --args="1 settings/default_settings.txt"
          ```
      *   On Windows:
          ```cmd
          gradlew run --args="1 config\Prophet\prophet_Movenment_perStrategyForwarding.txt"
          ```
          *(Note the use of backslashes `\` for paths on Windows Command Prompt/PowerShell and double quotes to enclose all arguments after `--args=`.)*

   *   **Example 2: Run Batch mode with a specific config file and multiple runs**
      *   On Linux:
          ```sh
          ./gradlew run --args="-b 1 settings/default_settings.txt"
          ```
      *   On Windows:
          ```cmd
          gradlew run --args="-b 1 settings/default_settings.txt"
          ```

   *   **Example 3: Run Batch mode and override settings using the -d option (if supported by your ONE version)**
      *   On Linux:
          ```sh
          ./gradlew run --args="-b 1:1 -d MovementModel.rngSeed=2@@Group.nrofHosts=3 default_settings.txt"
          ```
      *   On Windows:
          ```cmd
          gradlew run --args="-b 1:1 -d MovementModel.rngSeed=2@@Group.nrofHosts=3 default_settings.txt"
          ```
          *(Note that the arguments after `-d` are specific to your modified ONE version's functionality and should not contain spaces, using `@@` as a delimiter.)*

#### Running from a Distribution Package

The `installDist` task creates a complete, ready-to-run distribution package in a dedicated folder. This package includes platform-specific launcher scripts (`one.sh` for Linux/macOS, `one.bat` for Windows), the application JAR, and all necessary dependency JARs. This method is very similar to running the original ONE Simulator from an extracted release package and does not require Gradle or the wrapper to be present after the distribution is built.

1.  **Create the Distribution Package:**
    Use the `installDist` task via the Gradle Wrapper:
   *   On Linux:
       ```sh
       ./gradlew installDist
       ```
   *   On Windows:
       ```cmd
       gradlew installDist
       ```
    The distribution will be created in the `build/install/the-one-pitt` (or your project's name) folder at the project root.


### 🧹 Cleaning Build Artifacts

To remove all files and directories generated by the build process (the `build/` folder), use the `clean` task:

*   **On Linux:**
    ```sh
    ./gradlew clean
    ```
*   **On Windows:**
    ```cmd
    gradlew clean
    ```

---

## 📚 Additional Gradle Tasks (Optional)

Gradle provides several other useful tasks for project management:

*   `./gradlew tasks` / `gradlew tasks`: Displays a list of all available tasks.
*   `./gradlew test` / `gradlew test`: Runs unit tests (if enabled in `build.gradle`).
*   `./gradlew jar` / `gradlew jar`: Creates only the application JAR file in `build/libs/`. The `build` task already includes this step.
*   `./gradlew installDist` / `gradlew installDist`: Creates the full distribution package (as discussed above).
*   `./gradlew distZip` / `gradlew distZip`: Creates a `.zip` archive of the distribution package in `build/distributions/`.
*   `./gradlew distTar` / `gradlew distTar`: Creates a `.tar` archive of the distribution package in `build/distributions/`.
*   `./gradlew createJavadoc` / `gradlew createJavadoc`: Generates Javadoc documentation from the source code (if this custom task is enabled/defined in `build.gradle`). Output is in `build/docs/javadoc/`.

---

## 📂 Project Structure & Gradle Files

Key files and directories related to the Gradle build and project structure:

*   `build.gradle`: The primary build script defining project configuration, dependencies, and tasks. **Pre-configured for this project.**
*   `settings.gradle`: Defines the root project and any sub-projects (for this project, it mainly defines the root project name).
*   `gradle.properties`: Can be used for project-wide configuration properties (e.g., JVM arguments for Gradle itself).
*   `gradle/wrapper/`: Contains the Gradle Wrapper files (`gradle-wrapper.jar`, `gradle-wrapper.properties`) which manage the Gradle version.
*   `src/`: Contains the main Java source code of the ONE Simulator.
*   `lib/`: Contains local JAR library dependencies required by the project.
*   `config/`: Contains example and custom simulation configuration files (`.txt`).
*   `data/`: May contain input data files for simulations (e.g., mobility traces).
*   `build/`: The output directory for build artifacts (compiled classes, JARs, distributions). Automatically generated by Gradle.
*   `.gradle/`: Gradle's cache and daemon directory. Automatically generated by Gradle.

Simulation-specific configuration files (like `default_settings.txt` or those in `config/`) are separate and parsed by the ONE application itself, not by Gradle.

---

## 📌 Notes & Troubleshooting

*   Ensure **Java 17 or a compatible version** is installed and the `JAVA_HOME` environment variable is correctly set before running any Gradle commands.
*   If you encounter issues when running `./gradlew` or `gradlew.bat`, particularly for the first time or after modifying Gradle files, try executing the command with the `--stacktrace` option (e.g., `./gradlew build --stacktrace`) for more detailed error information.
*   All simulation configuration files in the `config/` folder can be customized to tailor your experiments without modifying the Gradle build configuration.
*   On Windows, running commands from `Command Prompt (cmd) as Administrator` might resolve certain file access or permission issues, although this is generally not necessary when using the Gradle Wrapper correctly.

---


<h1 align="center"> Happy Experimenting! </h1>