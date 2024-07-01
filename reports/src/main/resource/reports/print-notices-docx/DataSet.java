import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ru.intertrust.cm.core.service.api.ReportDS;
import ru.intertrust.cmj.af.collections.impl.Description;
import ru.intertrust.cmj.af.collections.impl.DescriptionCache;
import ru.intertrust.cmj.af.collections.impl.queue.PkdQueue;
import ru.intertrust.cmj.af.core.AFCMDomino;
import ru.intertrust.cmj.af.core.AFEntityStorage;
import ru.intertrust.cmj.af.core.AFObject;
import ru.intertrust.cmj.af.core.AFSession;
import ru.intertrust.cmj.af.utils.BeansUtils;
import ru.intertrust.cmj.pkd.collection.BuilderPkd;
import ru.intertrust.cmj.pkd.collection.BuilderPkdCommon;
import ru.intertrust.cmj.pkd.domain.PkdObject;
import ru.intertrust.cmj.pkd.service.PkdService;
import ru.intertrust.cmj.pkd.service.QueryParameters;
import ru.intertrust.cmj.pkd.service.impl.PkdServiceFactory;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class DataSet implements ReportDS {

    private PlatformTransactionManager txManager;
    private Environment environment;
    private BuilderPkdCommon pkdBuilder;

    private String baseUrl() {
        final String host = environment.getProperty("url.server.host.name", "http://localhost:8080");
        final String app = environment.getProperty("url.app.name", "cmj-web");

        return host + '/' + app;
    }

    private Map<BuilderPkd.QueryParamType, Map<String, Object>> queryParams() {
        Map<BuilderPkd.QueryParamType, Map<String, Object>> queryParams = new HashMap<>();
        queryParams.put(BuilderPkd.QueryParamType.FILTER, new HashMap<>());
        queryParams.put(BuilderPkd.QueryParamType.SORTING, new HashMap<>());
        queryParams.put(BuilderPkd.QueryParamType.OTHER, new HashMap<>());

        return queryParams;
    }

    private List<AFObject> getPkdObjects(List<String> pkdIds) {
        return pkdIds.stream()
                .map(pkdId -> (AFObject) AFEntityStorage.getEntityByUNID(pkdId))
                .collect(Collectors.toList());
    }

    private List<AFObject> getPkdObjects(String queryName, String personId, PkdQueue queue) {
        final PkdService service = PkdServiceFactory.getQueryService(queryName, personId);
        final QueryParameters queryParameters = new QueryParameters(queryParams(), 0, Integer.MAX_VALUE, null);

        List<? extends PkdObject> events = service.getResultList(queryParameters, queue);

        return events.stream().map(event -> (AFObject) event).collect(Collectors.toList());
    }

    private String getRuNoticeType(String noticeType) {
        switch (noticeType) {
            case "To register":
                return "На регистрацию";
            case "For execution":
                return "На исполнение";
            case "For control":
                return "На контроль";
            case "Addressing":
                return "На рассмотрение";
            case "Report for review":
                return "Отчёт на рассмотрение";
            default:
                return noticeType;
        }
    }

    private Object getPkdElementValue(@Nonnull String element, @Nonnull AFObject object) {
        Object createdValue = null;
        if (pkdBuilder != null) {
            try {
                createdValue = pkdBuilder.getElement(element, object);
            } catch (Exception ignore) {
            }
        }

        return createdValue;
    }

    @Override
    public JRDataSource getJRDataSource(Connection connection, Map params) throws Exception {

        final Object soReplicaIdParam = params.get("soReplicaId");
        if (!(soReplicaIdParam instanceof String)) {
            throw new IllegalArgumentException("Incorrect report parameter 'soReplicaId': " + soReplicaIdParam);
        }
        final Object personIdParam = params.get("personId");
        if (!(personIdParam == null || personIdParam instanceof String)) {
            throw new IllegalArgumentException("Incorrect report parameter 'personId': " + personIdParam);
        }
        final Object pkdViewNameParam = params.get("pkdViewName");
        if (!(pkdViewNameParam instanceof String)) {
            throw new IllegalArgumentException("Incorrect report parameter 'pkdViewName': " + pkdViewNameParam);
        }
        final Object pkdQueryNameParam = params.get("pkdQueryName");
        if (!(pkdQueryNameParam instanceof String)) {
            throw new IllegalArgumentException("Incorrect report parameter 'pkdQueryName': " + pkdQueryNameParam);
        }
        final Object pkdIdsParam = params.get("pkdIds");
        if (!(pkdIdsParam == null || pkdIdsParam instanceof List)) {
            throw new IllegalArgumentException("Incorrect report parameter 'pkdIds': " + pkdIdsParam);
        }
        if (pkdIdsParam == null && personIdParam == null) {
            throw new IllegalArgumentException("Report parameters 'pkdIds' or 'personId' is required");
        }

        boolean sessionManualOpen = false;
        TransactionStatus status = null;
        String oldSO = null;

        try {
            try {
                oldSO = AFCMDomino.getSODbReplicaID();
            } catch (Exception ignore) {

            }

            // Проверям открыта ли сессия. Если не открыта - открываем
            if (!AFSession.isDefinedOrOpened()) {
                AFSession.Manual.defineLocalUser();
                sessionManualOpen = true;
            }
            if (AFSession.get().isRunAsSystem()) {
                AFCMDomino.setCurrentOrganization((String) soReplicaIdParam);
            }

            final List<ReportRow> result = new ArrayList<>();

            // Открываем транзакцию или получаем существующую
            txManager = BeansUtils.getBean("transactionManager");

            final DefaultTransactionDefinition td = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
            td.setName(String.valueOf(new Date().getTime()));

            status = txManager.getTransaction(td);

            final Description description = DescriptionCache.getInstance().getDescription((String) pkdViewNameParam);
            pkdBuilder = new BuilderPkd(description);
            environment = BeansUtils.getBean("environment");
            if (AFSession.get().getAttribute("url-base", String.class).orElse(null) == null) {
                AFSession.get().setAttribute("url-base", baseUrl());
            }

            final List<AFObject> pkdObjects = pkdIdsParam != null
                    ? getPkdObjects(((List<?>) pkdIdsParam).stream().map(String::valueOf).collect(Collectors.toList()))
                    : getPkdObjects((String) pkdQueryNameParam, (String) personIdParam, description.getQueue());

            for (AFObject source : pkdObjects) {
                // Создан
                Object createdValue = getPkdElementValue("created", source);
                Calendar created = createdValue instanceof Calendar ? (Calendar) createdValue : null;

                // Тип уведомления
                Object noticeTypeValue = getPkdElementValue("noticeType", source);
                String noticeType = noticeTypeValue instanceof String ? (String) noticeTypeValue : "";
                String noticeTypeRusText = getRuNoticeType(noticeType);

                // Тип задачи
                Object taskTypeValue = getPkdElementValue("taskType", source);
                String taskType = taskTypeValue instanceof String ? (String) taskTypeValue : "";
                String taskTypeRusText = getRuNoticeType(taskType);

                // Номер
                Object regFullNumberValue = getPkdElementValue("regFullNumber", source);
                String regFullNumber = regFullNumberValue instanceof String ? (String) regFullNumberValue : "";

                // Подписант
                Object signerValue = getPkdElementValue("signer", source);
                Map<?, ?> signer = signerValue instanceof Map ? (Map<?, ?>) signerValue : null;
                String signerStr = "";
                if (signer != null) {
                    final StringBuilder signerStrBuilder = new StringBuilder();
                    for (Map.Entry<?, ?> entry : signer.entrySet()) {
                        if (entry.getKey().equals("shortName")) {
                            signerStrBuilder.append(entry.getValue()).append(", ");
                        }
                    }
                    signerStr = signerStrBuilder.toString();
                    signerStr = signerStr.substring(0, signerStr.length() - 2);
                }

                // Заголовок
                Object subjectValue = getPkdElementValue("subject", source);
                String subject = subjectValue instanceof String ? (String) subjectValue : "";

                // От кого
                Object fromValue = getPkdElementValue("from", source);
                Map<?, ?> from = fromValue instanceof Map ? (Map<?, ?>) fromValue : null;
                String fromStr = "";
                if (from != null) {
                    final StringBuilder fromStrBuilder = new StringBuilder();
                    for (Map.Entry<?, ?> entry : from.entrySet()) {
                        if (entry.getKey().equals("shortName")) {
                            fromStrBuilder.append(entry.getValue()).append(", ");
                        }
                    }
                    fromStr = fromStrBuilder.toString();
                    fromStr = fromStr.substring(0, fromStr.length() - 2);
                }

                // Кому
                Object toValue = getPkdElementValue("to", source);
                Map<?, ?> to = toValue instanceof Map ? (Map<?, ?>) toValue : null;
                String toStr = "";
                if (to != null) {
                    final StringBuilder toStrBuilder = new StringBuilder();
                    for (Map.Entry<?, ?> entry : to.entrySet()) {
                        if (entry.getKey().equals("shortName")) {
                            toStrBuilder.append(entry.getValue()).append(", ");
                        }
                    }
                    toStr = toStrBuilder.toString();
                    toStr = toStr.substring(0, toStr.length() - 2);
                }

                // Срок
                Object taskDueDateValue = getPkdElementValue("taskDueDate", source);
                Date taskDueDate = taskDueDateValue instanceof Date ? (Date) taskDueDateValue : null;

                // Текущий статус
                Object currentStateValue = getPkdElementValue("currentState", source);
                String currentState = currentStateValue instanceof String ? (String) currentStateValue : "";

                // Запланировано
                Object taskPlanDateValue = getPkdElementValue("taskPlanDate", source);
                Date taskPlanDate = taskPlanDateValue instanceof Date ? (Date) taskPlanDateValue : null;

                // Исполнитель
                Object taskExecutorValue = getPkdElementValue("taskExecutor", source);
                Map<?, ?> taskExecutor = taskExecutorValue instanceof Map ? (Map<?, ?>) taskExecutorValue : null;
                String taskExecutorStr = "";
                if (taskExecutor != null) {
                    StringBuilder taskExecutorStrBuilder = new StringBuilder();
                    for (Map.Entry<?, ?> entry : taskExecutor.entrySet()) {
                        if (entry.getKey().equals("shortName")) {
                            taskExecutorStrBuilder.append(entry.getValue()).append(", ");
                        }
                    }
                    taskExecutorStr = taskExecutorStrBuilder.toString();
                    taskExecutorStr = taskExecutorStr.substring(0, taskExecutorStr.length() - 2);
                }

                // Вид документа
                Object reqTypeValue = getPkdElementValue("reqType", source);
                String reqType = reqTypeValue instanceof String ? (String) reqTypeValue : "";

                // Адресаты
                Object addresseesValue = getPkdElementValue("addressees", source);
                Vector<?> addresseesVector = addresseesValue instanceof Vector ? (Vector<?>) addresseesValue : null;
                String addresseesStr = "";
                if (addresseesVector != null && !addresseesVector.isEmpty()) {
                    StringBuilder addresseesStrBuilder = new StringBuilder();
                    for (Object object : addresseesVector) {
                        Map<?, ?> addressees = object instanceof Map ? (Map<?, ?>) object : null;
                        if (addressees != null) {
                            for (Map.Entry<?, ?> entry : addressees.entrySet()) {
                                if (entry.getKey().equals("shortName")) {
                                    addresseesStrBuilder.append(entry.getValue()).append(", ");
                                }
                            }
                        }
                    }
                    addresseesStr = addresseesStrBuilder.toString();
                    addresseesStr = addresseesStr.substring(0, addresseesStr.length() - 2);
                }

                ReportRow row = new ReportRow();
                row.setCreated(created != null ? created.getTime() : null);
                row.setNoticeType(noticeTypeRusText);
                row.setTaskType(taskTypeRusText);
                row.setRegFullNumber(regFullNumber);
                row.setSigner(signerStr);
                row.setSubject(subject);
                row.setFrom(fromStr);
                row.setTo(toStr);
                row.setTaskDueDate(taskDueDate);
                row.setCurrentState(currentState);
                row.setTaskPlanDate(taskPlanDate);
                row.setTaskExecutor(taskExecutorStr);
                row.setReqType(reqType);
                row.setAddressees(addresseesStr);
                result.add(row);
            }
            // Если транзакция новая, созданная в этом методе, то закрываем. Она
            // нам больше не нужна
            if (status.isNewTransaction()) {
                txManager.commit(status);
            }

            return new JRBeanCollectionDataSource(result);
        } catch (Exception ex) {
            if (status != null && status.isNewTransaction()) {
                try {
                    txManager.rollback(status);
                } catch (Exception rollbackException) {
                    ex.addSuppressed(rollbackException);
                }
            }
            throw ex;
        } finally {
            if (AFSession.get().isRunAsSystem()) {
                AFCMDomino.setCurrentOrganization(oldSO);
            }
            // Если открывали сессию вручную, то закрываем
            if (sessionManualOpen) {
                AFSession.Manual.close();
            }
        }
    }

    public static class ReportRow {

        private Date created;
        private String noticeType;
        private String taskType;
        private String regFullNumber;
        private String signer;
        private String subject;
        private String from;
        private String to;
        private String currentState;
        private Date taskDueDate;
        private Date taskPlanDate;
        private String taskExecutor;
        private String reqType;
        private String addressees;

        public String getAddressees() {
            return addressees;
        }

        public void setAddressees(String addressees) {
            this.addressees = addressees;
        }

        public String getReqType() {
            return reqType;
        }

        public void setReqType(String reqType) {
            this.reqType = reqType;
        }

        public String getTaskExecutor() {
            return taskExecutor;
        }

        public void setTaskExecutor(String taskExecutor) {
            this.taskExecutor = taskExecutor;
        }

        public Date getTaskPlanDate() {
            return taskPlanDate;
        }

        public void setTaskPlanDate(Date taskPlanDate) {
            this.taskPlanDate = taskPlanDate;
        }

        public String getCurrentState() {
            return currentState;
        }

        public void setCurrentState(String currentState) {
            this.currentState = currentState;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public String getNoticeType() {
            return noticeType;
        }

        public void setNoticeType(String noticeType) {
            this.noticeType = noticeType;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String getRegFullNumber() {
            return regFullNumber;
        }

        public void setRegFullNumber(String regFullNumber) {
            this.regFullNumber = regFullNumber;
        }

        public String getSigner() {
            return signer;
        }

        public void setSigner(String signer) {
            this.signer = signer;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public Date getTaskDueDate() {
            return taskDueDate;
        }

        public void setTaskDueDate(Date taskDueDate) {
            this.taskDueDate = taskDueDate;
        }
    }
}
