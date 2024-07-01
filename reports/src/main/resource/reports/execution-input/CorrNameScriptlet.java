import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;


public class CorrNameScriptlet extends JRDefaultScriptlet {

		 final String RES_EXEC_STATE_ALL = "Все";
    	 final String RES_EXEC_STATE_EXEC = "Исполнено";
    	 final String RES_EXEC_STATE_NOT_EXEC = "Не исполнено";
    	 
    public String getCorrNames(Connection connection, Object idObjects) throws Exception {
        
        String executorName = "";

        String query = "SELECT " +
                                        " beard.orig_shortname as corr " +
                                        " FROM " +
                                        " SO_Beard beard WHERE  beard.id ";
        String endQuery = " IN (";
        ArrayList<Id> ids = (ArrayList<Id>) idObjects;
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
            endQuery += id.getId()+",";
        }
        endQuery += "-1)";

       
        PreparedStatement statement = connection.prepareStatement(query + endQuery);

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            if(executorName.equals("")){
                executorName = resultSet.getString("corr");
            }else{
                executorName += ", " + resultSet.getString("corr");
            }

        }
        statement.close();

        return executorName;
    }
    
    public String getDocCount(Connection connection, Object correspondents, Object controller, Date regDateStart, Date regDateEnd,  Date execDateStart, Date execDateEnd, Boolean contr, Object resExecState, Object thematicListParam) throws SQLException{
        String docCount="0";
        String query =" SELECT   "+
                 " count(DISTINCT rkk.id) as docCount  "+
                 " FROM F_DP_Resolution res                   "+
        		 " JOIN F_DP_ResltnBase resbase on resbase.id= res.id                   "+
        		 " JOIN F_DP_ResltnBase_ExecResp ON F_DP_ResltnBase_ExecResp.owner = res.id                   "+
        		 " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecResp.ExecutorResp                                  "+
        		 " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot                   "+
        		 " JOIN F_DP_InputRkk inputRkk on inputRkk.id = rkk.id                   "+
        		 " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id                          "+      
        		 " LEFT JOIN F_DP_RkkWORegAndCtrl_SMrk smrk on smrk.owner = inputRkk.id   "+
        		 " JOIN SO_Beard corr on corr.id = inputRkk.FromId   "+
        		 " JOIN SO_OrgDescriptionNonsys nonOrgsys on corr.id = nonOrgsys.beard       "+
        		 " LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+
        		 " WHERE     "+
				 " resbase.isdeleted <> 1 and res.IsProject <> 1 AND "+
        		 " resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND  "+
      		     " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND  "+
				 " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>) )   AND " +
        		" (<tematicNames>) " ;
        
        // whereQuery - хранит часть запроса с улсовием WHERE
        String whereQuery = " AND (rkk.regdate between ? AND  ? ) "
                + " AND (res.ctrldeadline between  ?  AND  ? )  ";
        
     // Список Контроллёров
        ArrayList<Id> ids = (ArrayList<Id>) controller;
        String controllers = " ";
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
            controllers += id.getId() + ",";
        }
        controllers += "-1 ";
        query = query.replaceAll("<controllersId>",controllers);

        // Список корреспондентов. Если пустой, то не добавляем в условие
        ids = (ArrayList<Id>) correspondents;
        if (ids != null && !ids.isEmpty()) {
            String correspondentsQuery = " AND nonOrgsys.id in (";
            for (Id id2 : ids) {
                RdbmsId id = (RdbmsId) id2;
                correspondentsQuery += id.getId() + ",";
            }
            correspondentsQuery += "-1) ";
            whereQuery += correspondentsQuery;
        }
		
        
        ArrayList<String> tematicList = (ArrayList<String>) thematicListParam;
        String tematics = "theme in (";
        if (thematicListParam!=null && !tematicList.isEmpty()) {
	        for (String tematic : tematicList) {
	        	tematics +="'" + tematic + "',";
	        }
	        tematics += "'-1') ";
        }
        else{
        	tematics="1=1";
        }
        query = query.replaceAll("<tematicNames>",tematics);
        
        String resExecStateQuery=" ";
        if (resExecState!=null) {
            if (((String) resExecState).equals(RES_EXEC_STATE_ALL)) {

            } else if (((String) resExecState).equals(RES_EXEC_STATE_EXEC)) {
                resExecStateQuery += " AND res.ctrldateexecution is not null";
            } else if (((String) resExecState).equals(RES_EXEC_STATE_NOT_EXEC)) {
                resExecStateQuery += " AND res.ctrldateexecution is null";
            }
            whereQuery += resExecStateQuery;
        }
		
        if (contr) {
            whereQuery += " AND rkk.RkkCtrlState > 0 ";
        }
        
        PreparedStatement statement = connection.prepareStatement(query + whereQuery);
        statement.setDate(1, new java.sql.Date(regDateStart.getTime()));
        statement.setDate(2, new java.sql.Date(regDateEnd.getTime()));
        statement.setDate(3, new java.sql.Date(execDateStart.getTime()));
        statement.setDate(4, new java.sql.Date(execDateEnd.getTime()));

        ResultSet resultSet = statement.executeQuery();
        
        while (resultSet.next()) {
            docCount =  String.valueOf(resultSet.getLong("docCount"));
        }
        statement.close();
        return docCount;
    }
}
