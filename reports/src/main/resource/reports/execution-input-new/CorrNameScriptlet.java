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
            endQuery += id.getId() + ",";
        }
        endQuery += "-1)";


        PreparedStatement statement = connection.prepareStatement(query + endQuery);

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            if (executorName.equals("")) {
                executorName = resultSet.getString("corr");
            } else {
                executorName += ", " + resultSet.getString("corr");
            }

        }
        statement.close();

        return executorName;
    }

    public String getDocCount(Connection connection, Object correspondents,
                              Object controller, Date regDateStart, Date regDateEnd,
                              Date execDateStart, Date execDateEnd, Boolean contr,
                              Object resExecStateParam, Object resExecDateStateParam,
                              Object thematicListParam) throws SQLException {
        String docCount = "0";
        String query = " SELECT " +
                " DISTINCT " +
                " docRegNum, " +
                " docRegDate, " +
                " docCorrespondent, " +
                " docNum, " +
                " docDate, " +
                " docSubject, " +
                " docDopControl, " +
                " resExecDate, " +
                " resSubject, " +
                " execBeardId, " +
                " beardtype, " +
                " resDeadline, " +
                " resId, " +
                " resExecutor, " +
                " docId, " +
                " docDeadline" +
                " FROM " +
                " ( " +
                " SELECT " +
                " CONCAT (rkk.regnumprist, rkk.regnumcnt, rkk.regnumfin) AS docRegNum, " +
                " rkk.regdate AS docRegDate, " +
                " rkk.id as docId, " +
                " rkk.ctrldeadline as docDeadline, " +
                " (case when inputRkk.FromId is null then inptrkk_plain.authorplain else corr.orig_shortname end) AS docCorrespondent," +
                " inputRkk.foreignnumber AS docNum, " +
                " inputRkk.foreignregdate AS docDate, " +
                " rkkBase.subject AS docSubject, " +
                " CASE WHEN  lower(trim(smrk.SpecMark)) = 'доп. контроль' THEN 'Д/К'    ELSE ''    END AS docDopControl, " +
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecResp.ExecutorResp=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, " +
                //subject resolution
                " resbase.resolution as resSubject, " +
                " beardExecutor.id as execBeardId, " +
                " beardExecutor.orig_type as beardtype, " +
                //executor resolution
                " beardExecutor.orig_shortname as resExecutor, " +

                " res.id as resId, " +
                " corr.id as docCorrespondentId, " +
                " res.ctrldeadline::date as resDeadline " +
                " FROM F_DP_Resolution res " +
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 " +
                " JOIN F_DP_ResltnBase_ExecResp ON F_DP_ResltnBase_ExecResp.owner = res.id " +
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecResp.ExecutorResp " +
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot " +
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id " +
                " JOIN F_DP_InputRkk inputRkk on inputRkk.id = rkkBase.id " +
                " LEFT JOIN F_DP_RkkWORegAndCtrl_SMrk smrk on smrk.owner = inputRkk.id " +
                " LEFT JOIN f_dp_inputrkk_authorplain inptrkk_plain on inputRkk.id=inptrkk_plain.owner" +
                " LEFT JOIN SO_Beard corr on corr.id = inputRkk.FromId " +
                " LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id " +
                " WHERE  " +
                " resbase.isdeleted <> 1 and res.IsProject <> 1 AND " +
                " resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND " +
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  AND  " +
                " (theme in <tematicNames>) " +
                " ) q";

        // whereQuery - хранит часть запроса с улсовием WHERE
        String whereQuery = " WHERE  " + " (docRegDate between ? AND ? ) "
                + " AND (resDeadline between ? AND ? )  ";

        // Список Контроллёров
        ArrayList<Id> ids = (ArrayList<Id>) controller;
        String controllers = " ";
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
            controllers += id.getId() + ",";
        }
        controllers += "-1 ";
        query = query.replaceAll("<controllersId>", controllers);

        // Список корреспондентов. Если пустой, то не добавляем в условие
         ids = (ArrayList<Id>) correspondents;
        if (ids != null && !ids.isEmpty()) {
            String correspondentsQuery = " ";
            for (Id id2 : ids) {
                RdbmsId id = (RdbmsId) id2;
                correspondentsQuery += id.getId() + ",";
            }
            correspondentsQuery += "-1 ";
            query = query.replaceAll("<CorrID>", correspondentsQuery);
        }

        ArrayList<String> tematicList = (ArrayList<String>) thematicListParam;
        String tematics = "(";
        if (tematicList != null && !tematicList.isEmpty()) {
            for (String tematic : tematicList) {
                tematics += "'" + tematic + "',";
            }
            tematics += "'-1') ";
        } else {
            tematics = "('-1') OR 1=1";
        }
        query = query.replaceAll("<tematicNames>", tematics);

        String resExecStateQuery = " ";
        String resExecState = (String) resExecStateParam;
        // Статус исполнения по дате
        String resExecDateState = (String) resExecDateStateParam;
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

        final String queryWrapper = "select count(distinct wrapper.docId) as docCount from (" +
                query +
                whereQuery +
                ") wrapper";

        PreparedStatement statement = connection.prepareStatement(queryWrapper);
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