package se.solrike.sonarlint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KotlinIntegrationTest {

  @TempDir
  Path mProjectDir;
  Path mBuildFile;

  @BeforeEach
  void setup() throws IOException {
    mBuildFile = Files.createFile(mProjectDir.resolve("build.gradle"));
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(mBuildFile.toFile()))) {
      writer.write("plugins { id('org.jetbrains.kotlin.jvm') version '1.6.21' \n  id('se.solrike.sonarlint')  }\n");
      // @formatter:off
      writer.write(""
          + "repositories {\n"
          + "  mavenCentral()\n"
          + "}\n");
      writer.write("\n"
          + "dependencies {\n"
          + "  implementation(platform('org.jetbrains.kotlin:kotlin-bom'))\n"
          + "  implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8')\n"
          + "  implementation('com.google.guava:guava:31.0.1-jre')\n"
          + "  testImplementation('org.jetbrains.kotlin:kotlin-test')\n"
          + "  testImplementation('org.jetbrains.kotlin:kotlin-test-junit')\n"
          + "  sonarlintPlugins('org.sonarsource.kotlin:sonar-kotlin-plugin:2.13.0.2116')\n"
          + "}\n");
      // @formatter:on
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    // setup for kotlin project
    Files.createDirectories(mProjectDir.resolve("src/main/kotlin/"));

  }

  @Test
  void testSonarlintMain() throws IOException {
    // given a kotlin class with
    createKotlinFile(Files.createFile(mProjectDir.resolve("src/main/kotlin/Hello.kt")));

    // when sonarlintKotlinMain is run
    BuildResult buildResult = runGradle(false, List.of("sonarlintMain"));

    // then the gradle build shall fail
    assertThat(buildResult.task(":sonarlintMain").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    // and the 1 sonarlint rules violated are
    assertThat(buildResult.getOutput()).contains("1 SonarLint issue(s) were found.");
    assertThat(buildResult.getOutput()).contains("kotlin:S1481");

    System.err.println(buildResult.getOutput());

  }

  BuildResult runGradle(List<String> args) {
    return runGradle(true, args);
  }

  BuildResult runGradle(boolean isSuccessExpected, List<String> args) {
    GradleRunner gradleRunner = GradleRunner.create()
        .withDebug(true)
        .withArguments(args)
        .withProjectDir(mProjectDir.toFile())
        .withPluginClasspath(); // to get plugin under test found by the runner
    return isSuccessExpected ? gradleRunner.build() : gradleRunner.buildAndFail();
  }

  void createKotlinFile(Path javaFile) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile.toFile()))) {
      // @formatter:off
      writer.write("class Hello {\n"
                 + "  fun someLibraryMethod(): Boolean {\n"
                 + "    val i = 1\n"
                 + "    return true\n"
                 + "  }\n"
                 + "}\n");
      // @formatter:on
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
