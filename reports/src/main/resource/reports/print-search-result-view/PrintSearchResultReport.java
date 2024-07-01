import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;

import ru.intertrust.cm.core.business.api.ReportGeneratorService;
import ru.intertrust.cm.core.business.impl.report.ReportBuilderFormats;
import ru.intertrust.cm.core.config.model.ReportMetadataConfig;
import ru.intertrust.cm.core.model.ReportServiceException;
import ru.intertrust.cm.core.service.api.ReportGenerator;

public class PrintSearchResultReport implements ReportGenerator {

    private static final String PRINT_VIEW_NAME = "PRINT_VIEW_NAME";
    private static final String PRINT_COLUMNS = "PRINT_COLUMNS";
    private static final String PRINT_COLUMNS_DATA = "PRINT_COLUMNS_DATA";

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Override
    public InputStream generate(ReportMetadataConfig reportMetadata, File templateFolder,
            Map<String, Object> parameters) {
        try {
            String title = ((String)parameters.get(PRINT_VIEW_NAME));
            Map<String, String> columns = ((Map<String, String>)parameters.get(PRINT_COLUMNS));
            List<Map<String, Object>> data = ((List<Map<String, Object>>)parameters.get(PRINT_COLUMNS_DATA));
            InputStream reportFileStream = reportGeneratorService.generateXLS(title, columns, data);
            return reportFileStream;
        } catch (Exception ex) {
            throw new ReportServiceException("Error generate XLS report", ex);
        }
    }

    @Override
    public String getFormat() {
        return ReportBuilderFormats.XLS_FORMAT.getFormat();
    }

}
