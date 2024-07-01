

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm_sochi.scriptlets.GetBodyAsHtmlBean;

public class OutputPrnForm extends JRDefaultScriptlet {
    
    public String getBody (final Object id) {
        return (new GetBodyAsHtmlBean()).getBody(id);
    }

}