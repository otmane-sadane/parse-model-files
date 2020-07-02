package fr.nexity.avr.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.nexity.avr.tools.utils.Utils;

/**
 * Main class.
 *
 */
public class App {

  private static Properties props = new Properties();
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private static final String CONFIG_FILE_PATH = "conf.properties";
  private static final String MODEL_DIR_PROP_NAME = "app.models.folder";
  private static final String WALK_DEPTH = "app.walk.level";
  private static final String INPUT_SQL_FILE_PROP_NAME = "app.sql.input";
  private static final String SQL_GEN_FOLDER_PROP_NAME = "app.slq.generation.folder";
  private static final String SQL_GEN_FILE_PROP_NAME = "app.slq.generation.file";
  /*
   * this map will contain hole domain structure in following form : "table " , "listOfColumns"
   */
  static private Map<String, List<ImmutablePair<Integer, String>>> modelStructure = new HashMap<>();

  public static void main(String[] args) {
    /*
     * init program.
     */
    init();

    /*
     * construct model structure
     */
    buildModelStructure();

    /*
     * process sql file and adapt it to model
     */
    processSqlFile();
  }

  /**
   * init program
   */
  static void init() {
    try {
      logger.debug("starting initialization");
      props.load(App.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH));

      logger.debug("end initialization");
    } catch (IOException e) {
      logger.error(
          String.format("Initialization error : cant find properties file %s", CONFIG_FILE_PATH));
    }
  }

  static void buildModelStructure() {
    logger.debug("start deducing model structure from models");
    String modelsDir = props.getProperty(MODEL_DIR_PROP_NAME);
    int walkDepth = Integer.parseInt(props.getProperty(WALK_DEPTH));
    List<String> filesPathList = Utils.getAllModelFilesInDirectory(modelsDir, walkDepth);
    filesPathList.stream()//
        .map(Utils::fileToStringListNoWhiteSpaces)// transfrom each file to
                                                  // list of
        // String
        .forEach(App::getEntityStructureFromList);// extract entity structure and put it in the map
    displayAllTablesNames();
    logger.debug("end deducing model structure from models");
  }

  static void processSqlFile() {
    logger.debug("start transforming sql file");
    String inputFile = props.getProperty(INPUT_SQL_FILE_PROP_NAME);
    String generateSqlFileName = props.getProperty(SQL_GEN_FILE_PROP_NAME);
    String generateSqlDir = props.getProperty(SQL_GEN_FOLDER_PROP_NAME);
    List<String> initialSqlLines = Utils.fileToStringListKeepingWhiteSpaces(inputFile);
    List<String> processedSqlLines = App.reorderSqlFileStatements(initialSqlLines);
    Utils.writeListOfStringInFile(processedSqlLines, generateSqlDir + generateSqlFileName);
    logger.debug("end transforming sql file");
  }

  // void configureLogginLevel(Level level) {
  // Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
  // root.setLevel(level);
  // }


  static void displayAllTablesNames() {
    List<String> tableNames = modelStructure.keySet().stream().collect(Collectors.toList());
    logger.info("Tables names : {}", StringUtils.join(tableNames, ","));

  }

  static void getEntityStructureFromList(List<String> fileLines) {
    // vars
    boolean entityFounded = false;
    String entityName = "";
    List<ImmutablePair<Integer, String>> attributesList = new ArrayList<>();
    Integer attributeOrder = 1;
    // loop through the lines
    for (String fileLine : fileLines) {

      if (!entityFounded && fileLine.contains("@entity")) {
        entityName = readEntityLine(fileLine);

      } else if (fileLine.contains("@column")) {
        attributesList.add(readAttributeLine(fileLine, attributeOrder++));
      }
    }

    // add the result in the map
    modelStructure.put(entityName, attributesList);
  }

  static String readEntityLine(String fileLine) {
    String entityName = null;
    if (fileLine.contains("@entity")) { // entity line
      entityName = Utils.extractNameFromFileLine(fileLine).orElse(null);
      if (entityName == null) {
        logger.error("Strange behavior : cant find name in 'entity' line {} ", fileLine);
      }
    }
    return entityName;
  }

  static ImmutablePair<Integer, String> readAttributeLine(String fileLine, Integer order) {
    ImmutablePair<Integer, String> attributeWithOrder = null;
    String attributeName = null;
    if (fileLine.contains("@column")) { // attribute line
      attributeName = Utils.extractNameFromFileLine(fileLine).orElse(null);
      if (attributeName != null) {
        attributeWithOrder = new ImmutablePair<Integer, String>(order, attributeName);
      } else {
        logger.error("Strange behavior : cant find name in 'attribute' line {} ", fileLine);
      }
    }
    return attributeWithOrder;
  }

  static List<String> reorderCreateStatementLines(List<String> unorderedList) {
    List<String> newOrderdList = new ArrayList<String>(unorderedList);

    // save id line a part before reordering
    String idLine = newOrderdList.get(1).replace("bigint", "bigserial");
    newOrderdList.remove(1);

    // line 1 : table name
    String tableName = Utils.extractTableName(unorderedList.get(0)).get();
    // load corresponding entity structure
    List<ImmutablePair<Integer, String>> listAttributesInOrder = modelStructure.get(tableName);

    // move elements in unordered list to make them in correct order
    for (ImmutablePair<Integer, String> attribute : listAttributesInOrder) {
      // the correct order in model structure
      int indexLineDestination = attribute.getLeft();
      // order in the sql file
      int indexLineSource = Utils.findLineIndex(newOrderdList, attribute.getRight());

      String lineToMove = newOrderdList.get(indexLineSource);


      // remove line from actual place
      newOrderdList.remove(indexLineSource);
      // put the deleted line in its correct place
      newOrderdList.add(indexLineDestination, lineToMove);

    }
    // put id line in first
    newOrderdList.add(1, idLine);
    // final check on list
    newOrderdList = checkLinesOfCreateStatement(newOrderdList);
    return newOrderdList;
  }

  static List<String> checkLinesOfCreateStatement(List<String> linesList) {

    // remove empty lines
    List<String> linesListToReturn =
        linesList.stream().filter(s -> !s.isBlank()).collect(Collectors.toList());
    // check semi columns (exept first and 2 last line)
    for (int j = 1; j < linesListToReturn.size() - 2; j++) {
      String currentLine = linesListToReturn.get(j).stripTrailing();
      if (!currentLine.endsWith(",")) {
        linesListToReturn.set(j, currentLine.concat(","));
      }
    }
    // remove semi column in before last line (if present)
    String beforeLastLine = linesListToReturn.get(linesListToReturn.size() - 2);
    if (beforeLastLine.stripTrailing().endsWith(",")) {
      linesListToReturn.set(linesListToReturn.size() - 2, beforeLastLine.replace(",", ""));
    }
    return linesListToReturn;
  }

  static List<String> reorderSqlFileStatements(List<String> sqlLinesUnordered) {
    List<String> sqlLinesOrdered = sqlLinesUnordered.stream().collect(Collectors.toList());
    logger.debug("unorderd list size {}", sqlLinesUnordered.size());
    logger.debug("orderd list size {}", sqlLinesOrdered.size());
    int loopIncrement = 0;
    int startCreateTableIndex = -1;
    int endCreateTableIndex = -1;
    // we may have some tables without worresponding entity in model
    boolean tableNameExistsInModelStructure = true;
    for (String sqlLine : sqlLinesUnordered) {

      if ((startCreateTableIndex == -1)) { // we are looking for start of create statement
        Optional<String> tableName = Utils.extractTableName(sqlLine);
        if (tableName.isPresent() && !tableName.get().isBlank()) {
          startCreateTableIndex = loopIncrement;
          tableNameExistsInModelStructure =
              modelStructure.get(tableName.get()) != null ? true : false;
        }
      } else if (endCreateTableIndex == -1) { // we are looking for end of create statement
        if (sqlLine.contains(");")) {
          endCreateTableIndex = loopIncrement;
        }
      }

      if (startCreateTableIndex != -1 && endCreateTableIndex != -1) { // create statement located
        if (tableNameExistsInModelStructure) {
          // extract the create statement part
          List<String> createTableSqlUnordred =
              sqlLinesUnordered.subList(startCreateTableIndex, endCreateTableIndex + 1);
          // reorder statments
          List<String> createTableSqlOrdered =
              App.reorderCreateStatementLines(createTableSqlUnordred);
          // remove all unordered statements
          // IntStream.range(startCreateTableIndex, endCreateTableIndex + 1)
          // .forEach(i -> sqlLinesOrdered.remove(i));

          // remove unordered statements
          for (int delIncrement =
              startCreateTableIndex; delIncrement <= endCreateTableIndex; delIncrement++) {
            logger.debug("operation seq {} - elem deleted (orederd) : {} ", delIncrement,
                sqlLinesOrdered.get(delIncrement));
            logger.debug("operation seq {} - elem deleted (unordered) : {} ", delIncrement,
                sqlLinesOrdered.get(delIncrement));
            sqlLinesOrdered.remove(startCreateTableIndex);
          }
          // put the new ordered statements
          sqlLinesOrdered.addAll(startCreateTableIndex, createTableSqlOrdered);

        }
        // init loop vars
        startCreateTableIndex = -1;
        endCreateTableIndex = -1;
        tableNameExistsInModelStructure = true;
      }



      loopIncrement++;
    }
    return sqlLinesOrdered;
  }



}
