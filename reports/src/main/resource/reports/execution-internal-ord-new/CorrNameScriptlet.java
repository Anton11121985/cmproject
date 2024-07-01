import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;


public class CorrNameScriptlet extends JRDefaultScriptlet {
    final String RES_EXEC_STATE_ALL = "Все";
    final String RES_EXEC_STATE_EXEC = "Исполнено";
    final String RES_EXEC_STATE_NOT_EXEC = "Не исполнено";

    final String RES_EXEC_DATE_STATE_ALL = "Все";
    final String RES_EXEC_DATE_STATE_OVERDUE = "Просроченные";
    final String RES_EXEC_DATE_STATE_NOT_OVERDUE = "Непросроченные";

    public String getDocCount(Connection connection, Object sign, Object controller,
                              Date regDateStart, Date regDateEnd, Date execDateStart,
                              Date execDateEnd, Boolean contr, Object thematicListParam,
                              Object resExecStateParam, Object resExecDateStateParam) throws SQLException {
        String docCount = "0";
        String query = " SELECT " +
                " count(distinct docId) as docCount " +
                " FROM " +
                " ( " +
                " SELECT " +
                " rkk.id as docId, " +
                " beardExecutor.id as execBeardId, " +
                " rkk.ctrldeadline as docDeadline, " +
                " rkk.regdate AS docRegDate, " +
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecResp.ExecutorResp=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, " +
                " res.ctrldeadline::date as resDeadline, " +
                " res.id as resId " +
                " FROM F_DP_Resolution res " +
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 " +
                " JOIN F_DP_ResltnBase_ExecResp ON F_DP_ResltnBase_ExecResp.owner = res.id " +
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecResp.ExecutorResp " +
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot " +
                " JOIN F_DP_IntRkk intRkk on intRkk.id = rkk.id " +
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id " +
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner " +
                " LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id " +
                " WHERE " +
                " resbase.isdeleted <> 1 and res.IsProject <> 1 AND " +
                " resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>)) " +
                " <tematicNames> " +
                " <whereSign> " +
                " UNION " +
                " SELECT " +
                " rkk.id as docId, " +
                " beardExecutor.id as execBeardId, " +
                " rkk.ctrldeadline as docDeadline, " +
                " rkk.regdate AS docRegDate, " +
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecExt.ExecutorExt=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, " +
                " res.ctrldeadline::date as resDeadline, " +
                " res.id as resId " +
                " FROM F_DP_Resolution res " +
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 " +
                " JOIN F_DP_ResltnBase_ExecExt ON F_DP_ResltnBase_ExecExt.owner = res.id " +
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecExt.ExecutorExt " +
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot " +
                " JOIN F_DP_IntRkk intRkk on intRkk.id = rkk.id " +
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id " +
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner " +
                " LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id " +
                " WHERE " +
                " resbase.isdeleted <> 1 and res.IsProject <> 1 AND " +
                " resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  " +
                " <tematicNames> " +
                " <whereSign> " +
                " UNION " +
                " SELECT " +
                " rkk.id as docId, " +
                " beardExecutor.id as execBeardId, " +
                " rkk.ctrldeadline as docDeadline, " +
                " rkk.regdate AS docRegDate, " +
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecResp.ExecutorResp=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, " +
                " res.ctrldeadline::date as resDeadline, " +
                " res.id as resId " +
                " FROM F_DP_Resolution res " +
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 " +
                " JOIN F_DP_ResltnBase_ExecResp ON F_DP_ResltnBase_ExecResp.owner = res.id " +
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecResp.ExecutorResp " +
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot " +
                " JOIN F_DP_OrdRkk intRkk on intRkk.id = rkk.id " +
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id " +
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner " +
                " LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id " +
                " WHERE " +
                " resbase.isdeleted <> 1 and res.IsProject <> 1 AND " +
                " resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  " +
                " <tematicNames> " +
                " <whereSign> " +
                " UNION " +
                " SELECT " +
                " rkk.id as docId, " +
                " beardExecutor.id as execBeardId, " +
                " rkk.ctrldeadline as docDeadline, " +
                " rkk.regdate AS docRegDate, " +
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecExt.ExecutorExt=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, " +
                " res.ctrldeadline::date as resDeadline, " +
                " res.id as resId " +
                " FROM F_DP_Resolution res " +
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 " +
                " JOIN F_DP_ResltnBase_ExecExt ON F_DP_ResltnBase_ExecExt.owner = res.id " +
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecExt.ExecutorExt " +
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot " +
                " JOIN F_DP_OrdRkk intRkk on intRkk.id = rkk.id " +
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id " +
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner " +
                " LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id " +
                " WHERE " +
                " resbase.isdeleted <> 1 and res.IsProject <> 1 AND " +
                " resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  " +
                " <tematicNames> " +
                " <whereSign> " +
                " ) q ";

        // whereQuery - хранит часть запроса с улсовием WHERE
        String whereQuery = " WHERE  " + " (docRegDate between  ? AND  ? ) "
                + " AND (resDeadline between  ?  AND  ? )  ";

        // Список Контроллёров
        ArrayList<Id> ids = (ArrayList<Id>) controller;
        String controllers = " ";
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
            controllers += id.getId() + ",";
        }
        controllers += "-1 ";
        query = query.replaceAll("<controllersId>", controllers);

        // Список подписантов
        ids = (ArrayList<Id>) sign;
        String whereSign = " ";
        if (ids != null && !ids.isEmpty()) {
            whereSign = " AND intRkk.SignSigner in (<signersId>)";
            String signIds = "";
            for (Id id2 : ids) {
                RdbmsId id = (RdbmsId) id2;
                signIds += id.getId() + ",";
            }
            signIds += "-1 ";
            whereSign = whereSign.replaceAll("<signersId>", signIds);
        }
        query = query.replaceAll("<whereSign>", whereSign);

        //Тематика
        ArrayList<String> tematicList = (ArrayList<String>) thematicListParam;
        String tematics = "AND theme in (";
        if (tematicList != null && !tematicList.isEmpty()) {
            for (String tematic : tematicList) {
                tematics += "'" + tematic + "',";
            }
            tematics += "'-1') ";
        } else {
            tematics = "";
        }
        query = query.replaceAll("<tematicNames>", tematics);

        //Параметр "Статус исполнения резолюций": исполнено/не исполнено/все. По умолчанию ВСЕ.
        String resExecState = (String) resExecStateParam;
        // Статус исполнения по дате
        String resExecDateState = (String) resExecDateStateParam;
        String resExecStateQuery = " ";
        if (resExecState == null || resExecState.equals(RES_EXEC_STATE_ALL)) {
            if (resExecDateState == null || resExecDateState.equals(RES_EXEC_DATE_STATE_ALL)) {
                // все - все
            } else if (resExecDateState.equals(RES_EXEC_DATE_STATE_OVERDUE)) {
                // все - просроченные
                resExecStateQuery += " AND ((resDeadline <= now()::date AND resExecDate is null AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid)) OR " +
                        " (resExecDate > resDeadline)) ";
            } else if (resExecDateState.equals(RES_EXEC_DATE_STATE_NOT_OVERDUE)) {
                // все - непросроченные
                resExecStateQuery += " AND ((resDeadline > now()::date AND resExecDate is null AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid)) OR " +
                        " (resExecDate <= resDeadline)) ";
            }
        } else if (resExecState.equals(RES_EXEC_STATE_EXEC)) {
            if (resExecDateState == null || resExecDateState.equals(RES_EXEC_DATE_STATE_ALL)) {
                // исполненные - все
                resExecStateQuery += " AND resExecDate is not null ";
            } else if (resExecDateState.equals(RES_EXEC_DATE_STATE_OVERDUE)) {
                // исполненные - просроченные
                resExecStateQuery += " AND resExecDate > resDeadline ";
            } else if (resExecDateState.equals(RES_EXEC_DATE_STATE_NOT_OVERDUE)) {
                // исполненные - непросроченные
                resExecStateQuery += " AND resExecDate <= resDeadline ";
            }
        } else if (resExecState.equals(RES_EXEC_STATE_NOT_EXEC)) {
            // resExecStateQuery += " AND resExecDate is null AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid)";
            if (resExecDateState == null || resExecDateState.equals(RES_EXEC_DATE_STATE_ALL)) {
                // неисполненные - все
                resExecStateQuery += " AND resExecDate is null AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid) ";
            } else if (resExecDateState.equals(RES_EXEC_DATE_STATE_OVERDUE)) {
                // неисполненные - просроченные
                resExecStateQuery += " AND resDeadline <= now()::date AND resExecDate is null AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid) ";
            } else if (resExecDateState.equals(RES_EXEC_DATE_STATE_NOT_OVERDUE)) {
                // неисполненные - непросроченные
                resExecStateQuery += " AND resDeadline > now()::date AND resExecDate is null AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid) ";
            }
        }
        whereQuery += resExecStateQuery;

        if (contr) {
            whereQuery += " AND docDeadline is not null";
        }

        PreparedStatement statement = connection.prepareStatement(query + whereQuery);
        statement.setDate(1, new java.sql.Date(regDateStart.getTime()));
        statement.setDate(2, new java.sql.Date(regDateEnd.getTime()));
        statement.setDate(3, new java.sql.Date(execDateStart.getTime()));
        statement.setDate(4, new java.sql.Date(execDateEnd.getTime()));

        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            docCount = String.valueOf(resultSet.getLong("docCount"));
        }
        statement.close();
        return docCount;
    }
}
