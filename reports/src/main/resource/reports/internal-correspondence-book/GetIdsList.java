import java.util.ArrayList;

import org.springframework.core.env.Environment;

import ru.intertrust.cm_sochi.srv.connector.sochi.reports.BaseReportScriptlet;
import ru.intertrust.cm.core.business.api.CrudService;
import ru.intertrust.cm.core.business.api.dto.DomainObject;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;
import ru.intertrust.cm_sochi.srv.connector.sochi.IdConverter;
import ru.intertrust.cmj.af.utils.BeansUtils;

public class GetIdsList extends BaseReportScriptlet {
	
	public String getIdsList(Object idsList) {
        ArrayList<Id> ids = null;
        if( idsList == null ) {
            ids = new ArrayList<>();
        } else if ( idsList instanceof ArrayList ) {
            ids = (ArrayList<Id>) idsList;
        } else if ( idsList instanceof Id ) {
            ids = new ArrayList<>();
            ids.add((Id)idsList);
        }
        String controllers = " ";
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
             controllers += id.getId() + ",";
        }
		controllers += "-1 ";

		return controllers;
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
