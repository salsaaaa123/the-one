plugins {
    application
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

application {
    mainClass.set("core.DTNSim")  // Pastikan ini sesuai dengan class utama
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
    java.srcDirs("src", "src/routing/communitypeople")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "core.DTNSim"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.named<JavaExec>("run") {
    dependsOn("classes")  // Pastikan semua kelas terkompilasi sebelum dijalankan
    jvmArgs = listOf("-Xmx512M")  // Alokasi memori yang cukup

    classpath = sourceSets.main.get().runtimeClasspath  // Pakai classpath yang benar dari Gradle
/*
* Jangan anda ganti ini
* args = listOf("1", "default_settings.txt")  // Sesuaikan dengan eksekusi GUI
*/
    // args = listOf("1", "default_settings.txt")
    // args = listOf("1", "config/Prophet/prophet_Movenment.txt")
    // args = listOf("-b","200", "config/Prophet/prophet_Movenment_perStrategyForwarding.txt")
//    args = listOf("-b","11", "config/Prophet/prophet_Movenment_perStrategyForwardingMod.txt")
    // args = listOf("-b","11", "config/Prophet/prophet_Movenment_perStrategyForwarding.txt")
    args = listOf("-b","11", "config/Prophet/prophet_Movenment_perStrategyEpidemicForwardingMod.txt")



    

    // args = listOf("-b","20", "config/Prophet/prophet_Movenment_Forwarding.txt")
    // args = listOf("-b","7", "config/Prophet/prophet_Movenment_Haggle.txt")
    // args = listOf("-b","7", "config/Prophet/prophet_Movenment_HaggleMod.txt")

    // args = listOf("-b","4", "config/Prophet/prophetPaper_Movenment.txt")



//    args = listOf("-b","1", "default_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/Epidemic/EpidemicDecisionEngine_randomwaypoint_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/SprayAndWait/SprayAndWaitDecisionEngine_randomwaypoint_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/Prophet/ProphetDecisionEngine_randomwaypoint_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/SprayAndFocus/SprayAndFocusDecisionEngineDuration_random_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/SprayAndFocus/SprayAndFocusDecisionEngineDuration_human_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/SprayAndFocus/SprayAndFocusRouterRandom_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/SprayAndFocus/SprayAndFocusRouterHuman_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/PeopleRank/test1.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/PeopleRank/test.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/PeopleRank/DistributedPeopleRank_random_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/bublerap/bublerap_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/bublerap/settings_reality-bubblerap.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/PeopleRank/PeopleRankDecisionEngine_randomwaypoint_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/Prophet/prophet_settings_dec.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/Prophet/prophet2_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","1", "config/Prophet/eunhaklee-prophetplus.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","6", "config/Prophet/prophetRandom_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","6", "config/Prophet/prophetOriginal_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("1", "config/Prophet/prophetOriginal_settings.txt")  // Sesuaikan dengan eksekusi GUI
//   args = listOf("-b","6", "config/Prophet/prophetOriginal_Haggle_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("-b","6", "config/Prophet/prophetRandom_Haggle_settings.txt")  // Sesuaikan dengan eksekusi GUI
//    args = listOf("1", "config/Prophet/prophetRandom_Haggle_settings.txt")  // Sesuaikan dengan eksekusi GUI
}











