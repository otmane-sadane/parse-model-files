package fr.nexity.avr.tools.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  public static Optional<String> extractNameFromFileLine(String fileLine) {
    Pattern pattern = Pattern.compile("name=\"(.*?)\"");
    Matcher matcher = pattern.matcher(fileLine);
    String result = "";
    if (matcher.find()) {
      result = matcher.group(1);
    }
    return Optional.of(result);
  }

  public static List<String> getAllModelFilesInDirectory(String directoryPath, int level) {
    try (Stream<Path> walk = Files.walk(Paths.get(directoryPath), level)) {
      return walk.map(x -> x.toString())//
          .filter(f -> f.endsWith(".java"))//
          .collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * transform file to list of strings lower cased without empty spaces.
   * 
   * @param filePath
   * @return
   */
  public static List<String> fileToStringListNoWhiteSpaces(String filePath) {
    List<String> fileLines = null;
    try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
      fileLines = lines//
          .map(line -> line.replaceAll("\\s", ""))// remove white space from all lines
          // .filter(s -> s.equals(""))// remove empty lines
          .map(s -> s.toLowerCase())//
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error(String.format("Error reading file ", filePath));
    }
    return fileLines;
  }

  public static List<String> fileToStringListKeepingWhiteSpaces(String filePath) {
    List<String> fileLines = null;
    try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
      fileLines = lines.map(s -> s.toLowerCase())//
          .filter(s -> !s.startsWith("copy") && !s.startsWith("\\.")
          // && !s.isBlank()
              && !s.startsWith("--"))//
          // .map(s -> s.replace("public.", ""))
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error(String.format("Error reading file ", filePath));
    }
    return fileLines;
  }

  public static int findLineIndex(List<String> linesList, String searchedString) {
    OptionalInt indexOfSerchedElement = IntStream.range(0, linesList.size())
        .filter(i -> linesList.get(i).contains(searchedString)).findFirst();
    return indexOfSerchedElement.orElse(-1);
  }

  public static Optional<String> extractTableName(String fileLine) {
    Pattern pattern = Pattern.compile("create table public[.](.*?)(\\s*)\\(");
    Matcher matcher = pattern.matcher(fileLine);
    String result = "";
    if (matcher.find()) {
      result = matcher.group(1);
    }
    return Optional.of(result);
  }

  public static void writeListOfStringInFile(List<String> listOfLines, String fileName) {
    Path out = Paths.get(fileName + "-" + Instant.now().toEpochMilli() + ".sql");
    try {
      Files.write(out, listOfLines, Charset.defaultCharset());
    } catch (IOException e) {
      logger.error(e.getMessage());
    }
  }
}
