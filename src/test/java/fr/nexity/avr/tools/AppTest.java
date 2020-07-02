package fr.nexity.avr.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.nexity.avr.tools.utils.Utils;


/**
 * Unit test for simple App.
 */
public class AppTest {

  private static final Logger logger = LoggerFactory.getLogger(AppTest.class);

  @Test
  public void testMatcher() {
    String mydata = "@entity(name=\"test_name\",blabla=\"hghghg\")";
    try {
      Optional<String> name = Utils.extractNameFromFileLine(mydata);
      assertEquals("test_name", name.get());
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  @Test
  public void test() {
    Integer i = 1;
    Integer j = 1;
    logger.debug("++i {}", ++i);
    logger.debug("j++ {}", j++);
    logger.debug("i {}", i);
    logger.debug("j {} ", j);

  }

  @Test
  public void givenElementExists_AndIndexIs2_WhenSerchingElement_Return2() {
    assertEquals(2, Utils.findLineIndex(List.of("aaaa", "aabbbaa", "aaccaa", "oooiiii"), "cc"));
  }

  @Test
  public void when_ExtractingTableName_thenSuccess() {
    String tableLineInSqlFile = "create table public.associations_history_lot(";

    assertEquals("associations_history_lot", Utils.extractTableName(tableLineInSqlFile).orElse(""));
  }

  @Test
  public void givenUnorderedList_whenOredering_thenSuccess() {
    List<String> unorderedList = List.of("create table public.associations_history_lot (",
        "    id bigint not null,", "    deletion_flag boolean,", "    version bigint not null,",
        "    creation_identity character varying(255),",
        "    creation_system character varying(255),",
        "    creation_time timestamp without time zone,",
        "    last_update_identity character varying(255),",
        "    last_update_system character varying(255),",
        "    last_update_time timestamp without time zone,",
        "    association_date timestamp without time zone not null,",
        "    association_type character varying(20) not null,",
        "    ref_associations_history character varying(20) not null,",
        "    ref_lot character varying(20) not null,",
        "    ref_user character varying(20) not null,",
        "    association_history_ref_lot character varying(20)", ");");
    App.init();
    App.buildModelStructure();

    List<String> orderedList = App.reorderCreateStatementLines(unorderedList);

    assertNotEquals(orderedList.get(1), unorderedList.get(1));



  }
}
