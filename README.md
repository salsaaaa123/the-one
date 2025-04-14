<h1 align="center">🚀 The ONE v1.4.1 📡</h1> 
<p align="center">The ONE (Opportunistic Network Environment) is a simulator designed to research and analyze delay-tolerant networks (DTN). With this tool, you can:</p> <p align="center"> 🗺️ Generate mobility traces for nodes in a simulation <br> 💬 Run message exchange simulations using various DTN routing protocols <br> 📊 Visualize simulation results in real-time and analyze them after the simulation ends </p>

---

## 📖 Introduction

Before using this repository, it is recommended that you first understand the basic concepts of the ONE Simulator by trying out the following practice repository:

🔗 **Getting Started:** [Opportunistic Network Environment](https://github.com/hendrowunga/Opportunistic-Network-Environment.git)

This practice repository will help you grasp the fundamentals of using the ONE Simulator before proceeding to more advanced analysis and experimentation.

Once you understand the basics, you can begin conducting experiments and analyzing simulation results through graphical reports.

📂 **Simulation Reports:** [report](https://github.com/hendrowunga/Opportunistic-Network-Environment/tree/main/src/report)

📊 **Analysis & Graphs: ** [Algorithmic Comparison](https://github.com/hendrowunga/Opportunistic-Network-Environment/tree/main/discussion/AlgorithmicComparison)

---

## 🏁 Quick Start

### ⚙️ Compiling
To run the ONE Simulator from source code, you need to compile it using  **Gradle 8.10**. Make sure **Java 23 or newer**  is installed before proceeding.

#### **For Linux/macOS:**
```sh
  ./gradlew clean build
```
or
```sh
  ./gradlew build
```


#### **For Windows:**
```sh
  gradlew.bat clean build
```
or
```sh
  gradlew.bat build
```

If you're using  **IntelliJ IDEA**, be sure to add the following JAR libraries to the build path to ensure successful compilation:

1. Go to: `Project Structure` -> `Dependencies`
2. CLick the `+ ( Add )`
3. Select `JARs or Directories`
4. Add the following files from the `lib/` folder:
    - `DTNConsoleConnection.jar`
    - `ECLA.jar`
    - `junit-4.8.2.jar`
    - `uncommons-maths-1.2.1.jar`
5. Click "OK"

---

### 🏃 Running

ONE can be run using the provided scripts in this repository.

#### **Linux/macOS:**
```sh
    ./gradlew run
```

#### **Windows:**
```sh
    gradlew.bat run
```

To run simulations in batch mode (without a graphical interface):
```sh
    ./gradlew run -b 1 default_settings.txt
```

**Available Parameters:**
- `-b <number>`: Runs the simulation in batch mode without GUI.
- `conf-files`: The name of the configuration file used in the simulation.

---

### 🛠️ Configuring

Simulations in ONE are controlled by configuration files. These are plain text files with `key = value` formatting that allow you to set various simulation parameters.

Example of setting random seeds:
```ini
    MovementModel.rngSeed = [1; 2; 3; 4; 5]
```
The configuration above will run the simulation with 5 different seeds to vary the simulation scenarios.

To view complete configuration examples, refer to the `default_settings.txt` and `snw_comparison_settings.txt` files in this repository.

---

### 🔢 Run Indexing

Run indexing allows you to execute multiple configurations from a single configuration file. For example, to run simulations with different seed values:
```ini
    MovementModel.rngSeed = [1; 2; 3; 4; 5]
```
Use the following command to run the simulation in batch mode:
```sh
    ./gradlew run -b 5 my_config.txt
```
This will automatically run the simulation using each seed value specified.

---

## 📜 Dependencies

This project uses the following dependencies to ensure smooth simulation execution:

```gradle
plugins {
    id 'java'
    id 'application'
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(23))
}

application {
    mainClass.set("core.DTNSim")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("lib/ECLA.jar"))
    implementation(files("lib/DTNConsoleConnection.jar"))
    implementation(files("lib/junit-4.8.2.jar"))
    implementation(files("lib/uncommons-maths-1.2.1.jar"))
}

sourceSets.main {
    java.srcDirs("src")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "core.DTNSim"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.named<JavaExec>("run") {
    dependsOn("classes")
    jvmArgs = listOf("-Xmx512M")
    classpath = sourceSets.main.get().runtimeClasspath
    args = listOf("1", "default_settings.txt")
}
```

Make sure all required dependencies are placed inside the `lib/` folder before running the project.

---

## 📌 Notes

- Make sure `Java 23 or newer` is installed to use this project.

- If you experience issues on Windows, try running commands from `Command Prompt (cmd) as Administrator`.

- All configuration files can be customized as needed to tailor the simulation.


<h1 align="center"> Happy Experimenting! </h1> 

