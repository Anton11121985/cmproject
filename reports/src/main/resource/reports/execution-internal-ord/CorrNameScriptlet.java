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
  
    public String getDocCount(Connection connection, Object sign, Object controller, Date regDateStart, Date regDateEnd,  Date execDateStart, Date execDateEnd, Boolean contr, Object thematicListParam) throws SQLException{
        String docCount="0";
        String query =" SELECT "+ 
                " count(DISTINCT docId) as docCount "+
                " FROM "+
                " ( "+
                " SELECT "+  
                " DISTINCT rkk.id as docId, "+
                " rkk.regdate as docRegDate, "+
                " res.ctrldeadline as resDeadline, "+
                " rkk.ctrldeadline as docDeadline, "+
                " beardController.id as resController, "+
                " intRkk.SignSigner as docSignId "+
                " FROM F_DP_Resolution res "+    
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id "+    
                " JOIN f_dp_resltnbase_execcurr ON f_dp_resltnbase_execcurr.owner = res.id "+    
                " JOIN so_beard beardExecutor on beardExecutor.id =  f_dp_resltnbase_execcurr.ExecutorCurr "+    
                " JOIN F_DP_ResltnBase_Cntrller ON F_DP_ResltnBase_Cntrller.owner = res.id "+    
                " JOIN SO_Beard beardController on beardController.id = F_DP_ResltnBase_Cntrller.Controller "+    
                " LEFT JOIN SO_Appointment so_app on so_app.beard = beardController.id "+    
                " LEFT JOIN SO_Role roleContr on roleContr.beard = beardController.id "+     
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot "+    
                " JOIN F_DP_IntRkk intRkk on intRkk.id = rkk.id     "+
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id "+    
                " LEFT JOIN SO_Appointment app_sign on app_sign.beard = intRkk.SignSigner "+
                " LEFT JOIN SO_Role roleSign on roleSign.Beard = intRkk.SignSigner "+ 
				" LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+
				" WHERE " +
				" resbase.isdeleted <> 1 and res.IsProject <> 1 "+
        		" <tematicNames> "+
                " UNION "+
                " SELECT "+  
                " DISTINCT rkk.id as docId, "+
                " rkk.regdate as docRegDate, "+
                " res.ctrldeadline as resDeadline, "+
                " rkk.ctrldeadline as docDeadline, "+
                " beardController.id as resController, "+
                " ordRkk.SignSigner as docSignId "+
                " FROM F_DP_Resolution res "+    
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id "+    
                " JOIN f_dp_resltnbase_execcurr ON f_dp_resltnbase_execcurr.owner = res.id "+    
                " JOIN so_beard beardExecutor on beardExecutor.id =  f_dp_resltnbase_execcurr.ExecutorCurr "+    
                " JOIN F_DP_ResltnBase_Cntrller ON F_DP_ResltnBase_Cntrller.owner = res.id "+    
                " JOIN SO_Beard beardController on beardController.id = F_DP_ResltnBase_Cntrller.Controller "+    
                " LEFT JOIN SO_Appointment so_app on so_app.beard = beardController.id "+    
                " LEFT JOIN SO_Role roleContr on roleContr.beard = beardController.id "+     
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot "+    
                " JOIN F_DP_OrdRkk ordRkk on ordRkk.id = rkk.id "+    
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id "+    
                " LEFT JOIN SO_Appointment app_sign on app_sign.beard = ordRkk.SignSigner "+
                " LEFT JOIN SO_Role roleSign on roleSign.Beard = ordRkk.SignSigner "+ 
				" LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+
				" WHERE " +
				" resbase.isdeleted <> 1 and res.IsProject <> 1 "+
        		" <tematicNames> "+
                " )q ";
        
        // whereQuery - хранит часть запроса с улсовием WHERE
        String whereQuery = " WHERE  " + " (docRegDate between  ? AND  ? ) "
                + " AND (resDeadline between  ?  AND  ? )  ";
        
     // Список Контроллёров
     // Список Контроллёров
        ArrayList<Id> ids = (ArrayList<Id>) controller;
        String controllers = " AND resController in(";
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
            controllers += id.getId() + ",";
        }
        controllers += "-1) ";
        whereQuery += controllers;

        // Список подписантов
        ids = (ArrayList<Id>) sign;
        if (ids!=null && !ids.isEmpty()) {
            String signs = " AND docSignId in (";
            for (Id id2 : ids) {
                RdbmsId id = (RdbmsId) id2;
                signs += id.getId() + ",";
            }
            signs += "-1) ";
            whereQuery += signs;
        }
        
        if (contr){
            whereQuery +=" AND docDeadline is not null";
        }
		
		ArrayList<String> tematicList = (ArrayList<String>) thematicListParam;
        String tematics = "AND theme in (";
        if (tematicList!=null && !tematicList.isEmpty()) {
	        for (String tematic : tematicList) {
	        	tematics +="'" + tematic + "',";
	        }
	        tematics += "'-1') ";
        }
        else{
        	tematics="";
        }
        query = query.replaceAll("<tematicNames>",tematics);
        
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
