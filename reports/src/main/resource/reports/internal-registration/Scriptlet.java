import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;
import ru.intertrust.cmj.af.utils.BeansUtils;
import ru.intertrust.cm.core.business.api.CrudService;
import ru.intertrust.cm.core.business.api.dto.DomainObject;


public class Scriptlet extends JRDefaultScriptlet {
    public String getIdsList(Object object) throws Exception {
        String returnString = "";
        if(object instanceof RdbmsId)
        {
            RdbmsId id = (RdbmsId) object;
            returnString += id.getId() + ",";
            returnString += "-1";
        }
        else if(object instanceof ArrayList)
        {
            for (RdbmsId id: (ArrayList<RdbmsId>) object) {
                returnString += id.getId() + ",";
            }
            returnString += "-2";
        }
        else if(object == null)
        {
            returnString += "-4";
        }
        return returnString;
    }

    public String arrayToString(Object object) throws Exception {
        String returnString = "";
        if(object instanceof ArrayList) {
            ArrayList<String> list = (ArrayList<String>) object;
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (i == list.size() - 1) {
                        returnString += list.get(i).toString();
                    } else {
                        returnString += list.get(i).toString() + ", ";
                    }
                }
            } else {
                returnString += "Все";
            }
        } else {
            returnString += "Все";
        }
        return returnString;
    }

    public String getNameParameter(Object idObj) {

        final CrudService crudService = BeansUtils.getBean("crudService");
        String returnString = "";

        if (idObj == null) {
            return returnString;
        }

        if(idObj instanceof ArrayList && ((ArrayList)idObj).size() > 0) {
            for (int i = 0; i < ((ArrayList)idObj).size(); i++) {

                final Id id = (Id) ((ArrayList)idObj).get(i);

                final DomainObject obj = crudService.find(id);

                if (obj == null) {
                    break;
                }

                final String nameString = obj.getString("orig_shortname");

                if(i == ((ArrayList)idObj).size() - 1) {
                    returnString += nameString;
                } else {
                    returnString += nameString + ", ";
                }
            }
            returnString = returnString;
        } else {
            final Id id = (Id) idObj;

            final DomainObject obj = crudService.find(id);

            if (obj == null) {
                return null;
            }

            returnString = obj.getString("orig_shortname");
        }

        return returnString;
    }
}
