package liquibase.dbtest;

import liquibase.context.ExecutionContext;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.test.JUnitResourceAccessor;
import liquibase.changelog.ChangeLogParameters;

import org.junit.Test;

public class IntXMLChangeLogSAXParserTest {
    @Test
    public void sampleChangeLogs() throws Exception {
        ExecutionContext context = new ExecutionContext();
        new XMLChangeLogSAXParser().parse("changelogs/cache/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/db2/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/derby/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/firebird/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/h2/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/hsqldb/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/maxdb/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/mysql/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/oracle/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/pgsql/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/sybase/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/asany/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
        new XMLChangeLogSAXParser().parse("changelogs/unsupported/complete/root.changelog.xml", new ChangeLogParameters(context), new JUnitResourceAccessor(), context);
    }

}
