import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateRtmTable;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

public class ReportTest {
    public static void main(String[] args) {
        JiraConnect.configureConnection();
        final GenerateRtmTable table = new GenerateRtmTable("31423940", "S21", false, 50);
        table.generate();
    }
}
