import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;



public class Scriplet extends JRDefaultScriptlet {
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
    public int getIdsListEmpty(Object idsList) {

        ArrayList ArrayIdsList = null;

        if( idsList == null ) {
            return -1;
        }
        else if ( idsList instanceof ArrayList ) {
            ArrayIdsList = (ArrayList<Id>) idsList;
            if (ArrayIdsList.size() == 0)
                return -1;
            else
                return 0;
        }
        else if ( idsList instanceof Id && idsList == null)
            return -1;
        else
            return 0;
    }
}
