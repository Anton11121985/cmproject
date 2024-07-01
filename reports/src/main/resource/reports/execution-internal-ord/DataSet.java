
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.healthmarketscience.rmiio.RemoteInputStreamClient;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ru.intertrust.cm.core.business.api.AttachmentService;
import ru.intertrust.cm.core.business.api.CollectionsService;
import ru.intertrust.cm.core.business.api.dto.DateTimeValue;
import ru.intertrust.cm.core.business.api.dto.DomainObject;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObject;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObjectCollection;
import ru.intertrust.cm.core.business.api.dto.Value;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;
import ru.intertrust.cm.core.service.api.ReportDS;
import ru.intertrust.cm_sochi.srv.util.PlainTextExtractor;
import ru.intertrust.cmj.af.utils.BeansUtils;

public class DataSet implements ReportDS {
    
    private AttachmentService attachmentService = BeansUtils.getBean("attachmentService");
    private CollectionsService collectionService = BeansUtils.getBean("collectionsService");

    public JRDataSource getJRDataSource(Connection connection, Map params) throws SQLException  {               

        final String RES_EXEC_STATE_ALL = "Все";
   	    final String RES_EXEC_STATE_EXEC = "Исполнено";
   	    final String RES_EXEC_STATE_NOT_EXEC = "Не исполнено";
   	 
   	// Если исполнитель ФР, то ищем заместителя
        final String getSubstituteForRoleQuery = " SELECT " + 
                " beardExecutorSubstitute.orig_shortname||'(ФР)' as execName " + 
                " FROM SO_Role role  " + 
                " JOIN SO_Substitute substitute on substitute.owner = role.id " +     
                " JOIN SO_Substitute_Unit substituteUnit on substituteUnit.id = substitute.id " +     
                " JOIN SO_AppointmentPlain appSubstitute on appSubstitute.Post = substituteUnit.Substitute " +     
                " JOIN SO_Appointment apSubstitute on apSubstitute.id = appSubstitute.id  " + 
                " JOIN SO_Beard beardExecutorSubstitute on beardExecutorSubstitute.id = apSubstitute.Beard " +    
                " WHERE role.Beard = ?	   " + 
                " UNION " + 
                " SELECT  " + 
                " beardExecutorSubstitute.orig_shortname||'(ФР)'  as execName " + 
                " FROM SO_Role role  " + 
                " JOIN SO_Substitute substitute on substitute.owner = role.id " +     
                " JOIN SO_Substitute_Unit substituteUnit on substituteUnit.id = substitute.id " +       
                " JOIN SO_AppointmentHead aphSubstitute on aphSubstitute.Post = substituteUnit.Substitute " +     
                " JOIN SO_Appointment apSubstitute on apSubstitute.id = aphSubstitute.id     " + 
                " JOIN SO_Beard beardExecutorSubstitute on beardExecutorSubstitute.id = apSubstitute.Beard " +    
                " WHERE role.Beard = ?";
        
        // Запрос
        // 4 UNION:
        // 1 ВНД Внутренние исполнители резолюций
        // 2 ВНД Внешние исполнители резолюций
        // 3 ОРД Внутренние исполнители резолюций
        // 4 ОРД Внешние исполнители резолюций
        String query = " SELECT " + 
                " DISTINCT "+        
                " docRegNum, "+                   
                " docRegDate, "+                                                  
                " docSubject, "+                                            
                " resExecDate, "+                   
                " resSubject, "+                   
                " execBeardId, "+                   
                " execBeardType, "+                   
                " execBeardName, "+
                " signerBeardId, "+                   
                " signerBeardType, "+                   
                " signerBeardName, "+
                " resDeadline, "+                   
                " resId "+
                " FROM "+            
                " ( "+            
                " SELECT "+            
                " CONCAT (rkk.regnumprist, rkk.regnumcnt, rkk.regnumfin) AS docRegNum, "+        
                " rkk.regdate AS docRegDate, "+         
                " rkkBase.subject AS docSubject, "+        
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecResp.ExecutorResp=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, "+
                " resbase.resolution as resSubject, "+         
                " beardExecutor.id as execBeardId, "+        
                " beardExecutor.orig_type as execBeardType, "+         
                " beardExecutor.orig_shortname as execBeardName, "+
                " beardSigner.id as signerBeardId, "+        
                " beardSigner.orig_type as signerBeardType, "+         
                " beardSigner.orig_shortname as signerBeardName, "+
                " res.ctrldeadline as resDeadline, "+   
                " res.id as resId "+
                " FROM F_DP_Resolution res "+         
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 "+         
                " JOIN F_DP_ResltnBase_ExecResp ON F_DP_ResltnBase_ExecResp.owner = res.id "+         
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecResp.ExecutorResp "+         
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot "+         
                " JOIN F_DP_IntRkk intRkk on intRkk.id = rkk.id "+         
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id "+
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner "+    
				" LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+				
                " WHERE "+        
				" resbase.isdeleted <> 1 and res.IsProject <> 1 AND "+				
				" resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+
      		    " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+
				" res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>)) "+
                " <tematicNames> " +
				" <whereSign> "+
                " UNION "+
                " SELECT "+            
                " CONCAT (rkk.regnumprist, rkk.regnumcnt, rkk.regnumfin) AS docRegNum, "+        
                " rkk.regdate AS docRegDate, "+         
                " rkkBase.subject AS docSubject, "+        
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecExt.ExecutorExt=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, "+
                " resbase.resolution as resSubject, "+         
                " beardExecutor.id as execBeardId, "+        
                " beardExecutor.orig_type as execBeardType, "+         
                " beardExecutor.orig_shortname as execBeardName, "+
                " beardSigner.id as signerBeardId, "+        
                " beardSigner.orig_type as signerBeardType, "+         
                " beardSigner.orig_shortname as signerBeardName, "+
                " res.ctrldeadline as resDeadline, "+   
                " res.id as resId "+
                " FROM F_DP_Resolution res "+         
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 "+         
                " JOIN F_DP_ResltnBase_ExecExt ON F_DP_ResltnBase_ExecExt.owner = res.id "+         
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecExt.ExecutorExt "+         
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot "+         
                " JOIN F_DP_IntRkk intRkk on intRkk.id = rkk.id "+         
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id "+
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner "+          
				" LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+
                " WHERE "+       
				" resbase.isdeleted <> 1 and res.IsProject <> 1 AND "+
				" resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+
      		    " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+				
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  "+
				" <tematicNames> " +
                " <whereSign> "+
                " UNION "+
                " SELECT "+            
                " CONCAT (rkk.regnumprist, rkk.regnumcnt, rkk.regnumfin) AS docRegNum, "+        
                " rkk.regdate AS docRegDate, "+         
                " rkkBase.subject AS docSubject, "+        
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecResp.ExecutorResp=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, "+
                " resbase.resolution as resSubject, "+         
                " beardExecutor.id as execBeardId, "+        
                " beardExecutor.orig_type as execBeardType, "+         
                " beardExecutor.orig_shortname as execBeardName, "+
                " beardSigner.id as signerBeardId, "+        
                " beardSigner.orig_type as signerBeardType, "+         
                " beardSigner.orig_shortname as signerBeardName, "+
                " res.ctrldeadline as resDeadline, "+   
                " res.id as resId "+
                " FROM F_DP_Resolution res "+         
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 "+         
                " JOIN F_DP_ResltnBase_ExecResp ON F_DP_ResltnBase_ExecResp.owner = res.id "+         
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecResp.ExecutorResp "+         
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot "+         
                " JOIN F_DP_OrdRkk intRkk on intRkk.id = rkk.id "+         
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id "+
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner "+   
				" LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+				
                " WHERE "+        
				" resbase.isdeleted <> 1 and res.IsProject <> 1 AND "+
				" resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+
      		    " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+				
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  "+
				" <tematicNames> " +
                " <whereSign> "+
                " UNION "+
                " SELECT "+            
                " CONCAT (rkk.regnumprist, rkk.regnumcnt, rkk.regnumfin) AS docRegNum, "+        
                " rkk.regdate AS docRegDate, "+         
                " rkkBase.subject AS docSubject, "+        
                " coalesce((SELECT rep.execdate FROM F_DP_Report rep WHERE rep.isdeleted=0 AND rep.HierParent=res.id AND rep.approvedt IS NOT NULL AND F_DP_ResltnBase_ExecExt.ExecutorExt=rep.Author AND rep.approvestatus = 1 ORDER BY rep.approvedt ASC LIMIT 1)::date,res.CtrlDateExecution::date) AS ResExecDate, "+
                " resbase.resolution as resSubject, "+         
                " beardExecutor.id as execBeardId, "+        
                " beardExecutor.orig_type as execBeardType, "+         
                " beardExecutor.orig_shortname as execBeardName, "+
                " beardSigner.id as signerBeardId, "+        
                " beardSigner.orig_type as signerBeardType, "+         
                " beardSigner.orig_shortname as signerBeardName, "+
                " res.ctrldeadline as resDeadline, "+   
                " res.id as resId "+
                " FROM F_DP_Resolution res "+         
                " JOIN F_DP_ResltnBase resbase on resbase.id= res.id and resbase.isdeleted <> 1 "+         
                " JOIN F_DP_ResltnBase_ExecExt ON F_DP_ResltnBase_ExecExt.owner = res.id "+         
                " JOIN so_beard beardExecutor on beardExecutor.id =  F_DP_ResltnBase_ExecExt.ExecutorExt "+         
                " JOIN F_DP_Rkk rkk on rkk.id = res.HierRoot "+         
                " JOIN F_DP_OrdRkk intRkk on intRkk.id = rkk.id "+         
                " JOIN F_DP_RkkBase rkkBase on rkkBase.id = rkk.id "+
                " JOIN so_beard beardSigner on beardSigner.id =  intRkk.SignSigner "+ 
				" LEFT JOIN F_DP_RkkBase_Theme theme on theme.owner= rkk.id "+								
                " WHERE "+     
				" resbase.isdeleted <> 1 and res.IsProject <> 1 AND "+				
				" resbase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+
      		    " rkkBase.module not in (SELECT module.id FROM SS_Module module JOIN SS_ModuleType moduletype on moduletype.id = module.type WHERE moduletype.Alias='TempStorage') AND "+				
                " res.id  in (SELECT owner FROM F_DP_ResltnBase_Cntrller where Controller in (<controllersId>))  "+
				" <tematicNames> " +
                " <whereSign> "+
                " ) q ";       
        // whereQuery - хранит часть запроса с улсовием WHERE
        String whereQuery = " WHERE  " + " (docRegDate between  {0} AND {1} ) "
                + " AND (resDeadline between {2} AND {3} )  ";

        // Список Контроллёров
        ArrayList<Id> ids = (ArrayList<Id>) params.get("EMPL_LIST_CONTR.Value");
        String controllers = " ";
        for (Id id2 : ids) {
            RdbmsId id = (RdbmsId) id2;
            controllers += id.getId() + ",";
        }
        controllers += "-1 ";
        query = query.replaceAll("<controllersId>",controllers);

        // Список подписантов. Если пустой, то не добавляем в условие
        String whereSign = " ";
        ids = (ArrayList<Id>) params.get("EMPL_LIST_SIGN.Value");
        if (ids!=null && !ids.isEmpty()) {
            whereSign = " AND intRkk.SignSigner in (<signersId>)";
            String signIds="";
            for (Id id2 : ids) {
                RdbmsId id = (RdbmsId) id2;
                signIds += id.getId() + ",";
            }
            signIds += "-1 ";
            whereSign = whereSign.replaceAll("<signersId>",signIds);
        }
        query = query.replaceAll("<whereSign>",whereSign);
		
		//Тематика
		ArrayList<String> tematicList = (ArrayList<String>) params.get("TEMATIC.Name");
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

        //CMFIVE-6917 Параметр "Статус исполнения резолюций": исполнено/не исполнено/все. По умолчанию ВСЕ. 
        String resExecState = (String) params.get("RES_EXEC_STATE.Name");
        String resExecStateQuery=" ";
        if (resExecState!=null && resExecState.equals(RES_EXEC_STATE_ALL)){
        	
        }else if (resExecState.equals(RES_EXEC_STATE_EXEC)){
        	resExecStateQuery+=" AND resExecDate is not null";
        }else if (resExecState.equals(RES_EXEC_STATE_NOT_EXEC)){
        	resExecStateQuery+=" AND resExecDate is null  AND exists (Select execCtrld.id from F_DP_Resolution_ExecCtrld execCtrld where execCtrld.owner=q.resid and execCtrld.ExecutorControlled = q.execbeardid)";
        }
        whereQuery += resExecStateQuery;
        
        // Получаем все параметры из мапы
        Date regDateStart = (Date) params.get("DATE_REG.Start");
        Date regDateEnd = (Date) params.get("DATE_REG.End");
        Date execDateStart = (Date) params.get("DATE_EXEC.Start");
        Date execDateEnd = (Date) params.get("DATE_EXEC.End");
        
        List<Value> filter = new ArrayList<Value>();
        filter.add(new DateTimeValue(regDateStart));
        filter.add(new DateTimeValue(regDateEnd));
        filter.add(new DateTimeValue(execDateStart));
        filter.add(new DateTimeValue(execDateEnd));

        IdentifiableObjectCollection collection = collectionService.findCollectionByQuery(query + whereQuery,filter );
        List<ReportRow> result = new ArrayList<ReportRow>();
        for (IdentifiableObject o : collection) {
            ReportRow row = new ReportRow();
            row.setDocRegNum(o.getString("docRegNum"));
            row.setDocRegDate((o.getTimelessDate("docRegDate")!=null?o.getTimelessDate("docRegDate").toDate():null));
            
           //Получаем тип подписанта
            Long signerBeardType = o.getLong("signerBeardType");
            //Если тип подписанта - Роль
            if (signerBeardType==4){
            	//Пытаемся найти назначение на эту роль
            	PreparedStatement statement1 = connection.prepareStatement(getSubstituteForRoleQuery);
            	statement1.setObject(1, o.getReference("signerBeardId"));
            	statement1.setObject(2, o.getReference("signerBeardId"));
            	ResultSet resultSet1 = statement1.executeQuery();
            	//Если есть назначенный на эту Роль работник выводим его ФИО
            	if (resultSet1.next()){
            		row.setDocSign(resultSet1.getString("execName"));
            	//Иначе Выводим Имя Роли + (ФР) из основного запроса
            	}else{
            		row.setDocSign(o.getString("signerBeardName")+"(ФР)");
            	}
            	statement1.close();
            // Если тип подписанта НЕ Роль, выводим его ФИО
            }else{
            	row.setDocSign(o.getString("signerBeardName"));
            }
            row.setDocSubject(o.getString("docSubject"));
            row.setResControlDate((o.getTimelessDate("resDeadline")!=null?o.getTimelessDate("resDeadline").toDate():null));
          //Получаем тип исполнителя
            Long execBeardType = o.getLong("execBeardType");
            //Если тип исполнителя - Роль
            if (execBeardType==4){
            	//Пытаемся найти назначение на эту роль
            	PreparedStatement statement1 = connection.prepareStatement(getSubstituteForRoleQuery);
            	statement1.setObject(1, o.getReference("execBeardId"));
            	statement1.setObject(2, o.getReference("execBeardId"));
            	ResultSet resultSet1 = statement1.executeQuery();
            	//Если есть назначенный на эту Роль работник выводим его ФИО
            	if (resultSet1.next()){
            		row.setResExecutor(resultSet1.getString("execName"));
            	//Иначе Выводим Имя Роли + (ФР) из основного запроса
            	}else{
            		row.setResExecutor(o.getString("execBeardName")+"(ФР)");
            	}
            	statement1.close();
            // Если тип исполнителя НЕ Роль, выводим его ФИО
            }else{
            	row.setResExecutor(o.getString("execBeardName"));
            }
            row.setResExecDate((o.getTimelessDate("resExecDate")!=null?o.getTimelessDate("resExecDate").toDate():null));
            row.setResSubject(o.getString("resSubject") + "\n" + getAttachmentText(o.getReference("resId"), "F_ContentRichText_Res"));
            row.setResExecutorDep(getResExecutorDep(o.getReference("execBeardId"), o.getLong("execBeardType"),
                    connection));
            result.add(row);
        }
        // Сортируем колллекцию по группируемой колонке. Обязательно для
        // JasperReports
        Collections.sort(result, new CustomComparator());
        return new JRBeanCollectionDataSource(result);
    }

    public class CustomComparator implements Comparator<ReportRow> {
        @Override
        public int compare(ReportRow o1, ReportRow o2) {
        	return o1.getResExecutorDep().compareTo(o2.getResExecutorDep());
        }
    }

    public class ReportRow {
        private String docRegNum;
        private Date docRegDate;
        private String docSign;
        private String docSubject;
        private Date resControlDate;
        private String resExecutor;
        private Date resExecDate;
        private String resSubject;
        private String resExecutorDep;

        public String getDocRegNum() {
            return docRegNum;
        }

        public void setDocRegNum(String docRegNum) {
            this.docRegNum = docRegNum;
        }

        public Date getDocRegDate() {
            return docRegDate;
        }

        public void setDocRegDate(Date docRegDate) {
            this.docRegDate = docRegDate;
        }

        public String getDocSign() {
            return docSign;
        }

        public void setDocSign(String docSign) {
            this.docSign = docSign;
        }

        public String getDocSubject() {
            return docSubject;
        }

        public void setDocSubject(String docSubject) {
            this.docSubject = docSubject;
        }

        public Date getResControlDate() {
            return resControlDate;
        }

        public void setResControlDate(Date resControlDate) {
            this.resControlDate = resControlDate;
        }

        public String getResExecutor() {
            return resExecutor;
        }

        public void setResExecutor(String resExecutor) {
            this.resExecutor = resExecutor;
        }

        public Date getResExecDate() {
            return resExecDate;
        }

        public void setResExecDate(Date resExecDate) {
            this.resExecDate = resExecDate;
        }

        public String getResSubject() {
            return resSubject;
        }

        public void setResSubject(String resSubject) {
            this.resSubject = resSubject;
        }

        public String getResExecutorDep() {
        	if(resExecutorDep==null){
        		return "!!!НЕ НАЙДЕНО!!!";
        	}
            return resExecutorDep;
        }

        public void setResExecutorDep(String resExecutorDep) {
            this.resExecutorDep = resExecutorDep;
        }

    }

    /**
     * Получение вышестоящего Подразделения\РВЗ\Организации
     * @param executorBeardId
     * @param executorBeardType
     * @param connection
     * @return
     * @throws SQLException
     */
    private String getResExecutorDep(Object executorBeardId, Long executorBeardType, Connection connection) throws SQLException
              {
        String query = "";
        // В зависимости от типа Исполнителя получаем его подразделение, если его нет, то возвращаем имя исполнителя.
        switch (executorBeardType.intValue()) {
        // Назначение-РВЗ (SO_AppointmentHead);
        // Получаем вышестоящее Структурную единицу(Подразделение, Организация) или ШЕ(РВЗ)
        case 1:
            query = " Select     " +
                    " hBeard.orig_postname || ' '|| hBeard.orig_shortname as execName, " + 
                    " coalesce(pBeard.id,beardParent.id) as parentId, " +
                    " coalesce(pBeard.orig_type,beardParent.orig_type) as pType " +     
                    " FROM " +    
                    " SO_AppointmentHead ah " + 
                    " JOIN SO_Post p on p.id = ah.Post " +    
                    " JOIN SO_Appointment a on a.id = ah.id " + 
                    " JOIN SO_Beard hBeard on hBeard.id = a.Beard " + 
                    " LEFT JOIN SO_Parent_SU psu on psu.id = p.HierParent " + 
                    " LEFT JOIN SO_StructureUnit pDepSU on pDepSU.id = psu.Owner " + 
                    " LEFT JOIN SO_Beard pBeard on pBeard.id = pDepSU.Beard " + 
                    " LEFT JOIN SO_Parent_PH pph on pph.id = p.HierParent " +
                    " LEFT JOIN SO_PostHead phParent on phParent.id = pph.Owner " +   
                    " LEFT JOIN SO_AppointmentHead ahParent on ahParent.Post = phParent.id " +
                    " LEFT JOIN SO_Appointment aParent on aParent.id = ahParent.id " + 
                    " LEFT JOIN SO_Beard beardParent on beardParent.id =aParent.Beard " + 
                    " WHERE ahParent.AccessRedirect is null AND hBeard.id =   ";
            break;

        // Подразделение (SO_Department);
        // Получаем самостоятельное подразделение для данного
        case 2:
            return getResExecutorDepName(executorBeardId, connection);
            
        // Назначение работника (SO_AppointmentPlain);
        // Получаем вышестоящее Структурную единицу(Подразделение,
        // Организация) или ШЕ(РВЗ)
        case 3:
            query = " Select    " +
                    " plBeard.orig_postname || ' '|| plBeard.orig_shortname as execName,  " +
                    " coalesce(pBeard.id,beardParent.id) as parentId, " +
                    " coalesce(pBeard.orig_type,beardParent.orig_type) as pType " +
                    " FROM  " +
                    " SO_AppointmentPlain ap    " +
                    " JOIN SO_Post p on p.id = ap.Post  " +
                    " JOIN SO_Appointment a on a.id = ap.id " +
                    " JOIN SO_Beard plBeard on plBeard.id = a.Beard  " +
                    " LEFT JOIN SO_Parent_SU psu on psu.id = p.HierParent  " +
                    " LEFT JOIN SO_StructureUnit pDepSU on pDepSU.id = psu.Owner  " +
                    " LEFT JOIN SO_Beard pBeard on pBeard.id = pDepSU.Beard  " +
                    " LEFT JOIN SO_Parent_PH pph on pph.id = p.HierParent " +
                    " LEFT JOIN SO_PostHead phParent on phParent.id = pph.Owner    " +
                    " LEFT JOIN SO_AppointmentHead ahParent on ahParent.Post = phParent.id " +
                    " LEFT JOIN SO_Appointment aParent on aParent.id = ahParent.id  " +
                    " LEFT JOIN SO_Beard beardParent on beardParent.id =aParent.Beard  " +
                    " WHERE ahParent.AccessRedirect is null AND plBeard.id =   ";
            break;
        //Роль
        case 4:
            query = " Select     " +
                    " beard.orig_postname || ' '|| beard.orig_shortname as execName,  " +
                    " coalesce(parentBeard.orig_type,beard.orig_type) as pType, " +
                    " coalesce(parentBeard.id,beard.id) as parentId " +
                    " FROM   " +  
                    " SO_Beard beard " +
                    " JOIN SO_Role role on role.Beard = beard.id " +
                    " LEFT JOIN SO_Substitute substitute on substitute.owner = role.id " +
                    " LEFT JOIN SO_Substitute_Unit substituteUnit on substituteUnit.id = substitute.id " +
                    " LEFT JOIN SO_AppointmentPlain app on app.Post = substituteUnit.Substitute " +
                    " LEFT JOIN SO_AppointmentHead aph on aph.Post = substituteUnit.Substitute " +
                    " LEFT JOIN SO_Appointment ap on ap.id = app.id OR ap.id = aph.id " +
                    " LEFT JOIN SO_Beard parentBeard on parentBeard.id = ap.Beard " +
                    " WHERE beard.id =  ";
            break;
        // Несистемная Организация;
        case 10:
            query = " Select " + " beard.orig_shortname as execName, " + " beard.orig_type as pType " + " FROM "
                    + " SO_Beard beard " + " WHERE beard.id =  ";
            break;

        // Несистемный работник;
        case 11:
            query = " Select " + " CONCAT_WS(' ',beard.orig_postname,beard.orig_shortname) as execName, "
                    + " beard.orig_type as pType " + " FROM " + " SO_Beard beard " + " WHERE beard.id =  ";
            break;
        }
        String execName = "";
        PreparedStatement statement = connection.prepareStatement(query+getStrId(executorBeardId));
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            // Если тип исполнителя или вышестоящей СЕ исполнителя равен
            // 2(Подразделение)
            // то получаем самостоятельное подразделение для него
            Long pType = resultSet.getLong("pType");
            // инче исполнитель - сам объект
            execName = resultSet.getString("execName");
            // Id родительского Подразделения или Организации ИЛИ РВЗ
            Object parentId = resultSet.getObject("parentId");
            
            //CMFIVE-6848 Если текущий исполнитель РВЗ и родитель РВЗ то создаюем собственную группу для подчиненного РВЗ
            if (pType == 1 && executorBeardType==1){
                //Ничего не делаем так как уже выше установили execName = resultSet.getString("execName");
            }        
            //Если родитель Подразделение\Организация(СЕ) - то идём до самостоятельного подразделения
            else if (pType == 2) {
                return getResExecutorDepName(parentId, connection);
            }
            //Если родитель РВЗ, то смотрим его родителя.
            //Если родитель работник(такой случай возможен только для ФР(ролей), когда работник - наблюдатель)
            else if (pType == 1 || (pType == 3 && executorBeardType==4)){
                execName = getResExecutorDep(parentId,  pType, connection);
            }
            //Если непосредственный исполнитель(поэтому смотрим по родительскому типу) роль, то делаем приписку "ФР"
            if (pType==4){
                execName = "ФУНКЦИОНАЛЬНАЯ РОЛЬ "+ execName;
            }
            
        }
       
        statement.close();
        return execName;
    }

    /**
     * Получение имени подразделения по иерархии до самостоятельного. Если
     * родитель организация - возвращаем имя текущего подразделения
     * если родитель РВЗ - возвращаем имя текущего подразделения
     * 
     * @param depId - id бороды подразделения для которго нужно получить вышестоящее самостоятельное подразделение.
     * @param connection
     * @return
     * @throws SQLException
     */
    private String getResExecutorDepName(Object depId, Connection connection) throws SQLException {
        // Запрос
        final String query = " Select " 
                + " isIndependent, "
                + " depName, "
                + " parentId, "
                + " pType, "
                + " orgName, "
                + " currentDepId "
                + " FROM( "
                + " Select "
                + " IsIndependent as isIndependent, "  
                + " depBeard.orig_shortname as depName, " 
                + " pBeard.id as parentId, "  
                + " pBeard.orig_type as pType, " 
                + " depBeard.orgname as orgName, "
                + " depBeard.id as currentDepId " 
                + " FROM SO_Beard depBeard " 
                + " JOIN SO_StructureUnit depSU on depSU.Beard = depBeard.id " 
                + " JOIN SO_Department dep on dep.id = depSU.id " 
                + " JOIN SO_Parent_SU psu on psu.id = dep.HierParent " 
                + " JOIN SO_StructureUnit pDepSU on pDepSU.id = psu.Owner " 
                + " JOIN SO_Beard pBeard on pBeard.id = pDepSU.Beard "  
                + " UNION "
                + " Select "
                + " IsIndependent as isIndependent, "  
                + " depBeard.orig_shortname as depName, " 
                + " pBeard.id as parentId, "  
                + " pBeard.orig_type as pType, " 
                + " depBeard.orgname as orgName, "
                + " depBeard.id as currentDepId " 
                + " FROM SO_Beard depBeard " 
                + " JOIN SO_StructureUnit depSU on depSU.Beard = depBeard.id " 
                + " JOIN SO_Department dep on dep.id = depSU.id " 
                + " JOIN SO_Parent_PH parentPH on parentPH.id = dep.HierParent " 
                + " JOIN SO_Post parentPost on parentPost.id = parentPH.Owner "
                + " JOIN SO_AppointmentHead aph on aph.Post = parentPost.id "
                + " JOIN SO_Appointment ap on ap.id = aph.id "
                + " JOIN SO_Beard pBeard on pBeard.id = ap.Beard "
                + " )q "
                + " WHERE currentDepId =  ";
        String name = "";
        PreparedStatement statement = connection.prepareStatement(query + getStrId(depId));
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            // Имя текущего Подразделения
            name = resultSet.getString("depName");
            // Признак самостоятельности
            boolean isIndependent = resultSet.getBoolean("isIndependent");
            // Если текущее подразделение НЕ самостоятельное, то
            if (!isIndependent) {
                // Получаем родительский тип - организация\подразделение\РВЗ
                Long pType = resultSet.getLong("pType");
                // Если родитель организация, товозвращаем имя организации т.к. выше по иерархии идти некуда
                if (pType==0){
                    return resultSet.getString("orgName");
                }
                // Если родитель РВЗ , товозвращаем имя текущего подразделения
                else if (pType==1){
                    return name;
                }
                // Если родитель Подразделение, то ищем дальше до самостоятельного
                else{
                // Id родительского Подразделения или Организации
                RdbmsId parentId = (RdbmsId) resultSet.getObject("parentId");
                
                name = getResExecutorDepName(parentId, connection);
                }
            }
        }
        statement.close();
        // иначе возвращаем имя подразделения
        return name;

    }

    /**
     * Получение даты исполнения - дедлайна для резолюции 1 уровня.
     * Сейчас не используется, нужно было раньше.
     * @param resId
     * @param connection
     * @return
     * @throws SQLException
     */
    private Date getRootResControlDate(Object resId, Connection connection) throws SQLException {
        // Запрос
        final String query = " Select " + " hierparent as parent, " + " hierroot as root, "
                + " ctrldeadline as deadline " + " FROM " + " F_DP_Resolution WHERE id = ? ";
        Date deadline = null;
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, resId);
        ResultSet resultSet = statement.executeQuery();
        List<ReportRow> result = new ArrayList<ReportRow>();
        while (resultSet.next()) {
            RdbmsId parentId = (RdbmsId) resultSet.getObject("parent");
            RdbmsId rooId = (RdbmsId) resultSet.getObject("root");
            deadline = resultSet.getTimestamp("deadline");
            // Если родитель резолюции равен документу резолюции, то она 1-го
            // уровня.
            if (!parentId.equals(rooId)) {
                deadline = getRootResControlDate(resultSet.getObject("parent"), connection);
            }
        }
        statement.close();
        return deadline;

    }
    
    
    /**
     * JDBC драйвер не поддерживает работу с id в функциях
     * пример - coalesce(pBeard.id,beardParent.id) - будет возвращён Long
     * для этого написан данный утилитарный класс, который обрабатывает все id полученные из запроса.
     * @param id
     * @return
     */
    
    private String getStrId(Object id){
        if (id instanceof Long) {
            return String.valueOf(id);
        } else if (id instanceof RdbmsId) {
            return String.valueOf(((RdbmsId) id).getId());
        }
        return null;
    }
    
    private String getAttachmentText(Id docId, String attachmentType){
        StringBuffer buffer = new StringBuffer();
        for(DomainObject o : attachmentService.findAttachmentDomainObjectsFor(docId, attachmentType)){
            if(buffer.length() > 0){
                buffer.append("\n");
            }
            try{
                buffer.append(PlainTextExtractor.extractPlainText(RemoteInputStreamClient.wrap(attachmentService.loadAttachment(o.getId()))));
            } catch (Exception e){
                throw new RuntimeException();
            }
        }
        return buffer.toString();
    }
}
