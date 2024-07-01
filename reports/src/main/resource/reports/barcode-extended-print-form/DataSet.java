import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import ru.intertrust.cm.core.business.api.CollectionsService;
import ru.intertrust.cm.core.business.api.CrudService;
import ru.intertrust.cm.core.business.api.dto.BooleanValue;
import ru.intertrust.cm.core.business.api.dto.DomainObject;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObject;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObjectCollection;
import ru.intertrust.cm.core.business.api.dto.ReferenceValue;
import ru.intertrust.cm.core.business.api.dto.Value;
import ru.intertrust.cm.core.service.api.ReportDS;
import ru.intertrust.cm_sochi.srv.connector.sochi.IdConverter;
import ru.intertrust.cmj.af.barcode.BarcodeStampGenerationService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;
import static ru.intertrust.cmj.af.core.AFCMDomino.setCurrentOrganization;
import static ru.intertrust.cmj.af.core.AFSession.Manual.close;
import static ru.intertrust.cmj.af.core.AFSession.Manual.defineLocalUser;
import static ru.intertrust.cmj.af.core.AFSession.isDefinedOrOpened;
import static ru.intertrust.cmj.af.utils.BeansUtils.getBean;

public class DataSet implements ReportDS {

    private static final Logger log = getLogger("ru.intertrust.cmj.report.BarcodeExtendedPrintForm");

    // language=PostgreSQL
    private static final String REQUEST_QUERY = "SELECT rkkbase.id AS id\n" +
            "FROM f_dp_rkkbase rkkbase\n" +
            "         LEFT JOIN f_dp_rkk rkk\n" +
            "                   ON rkkbase.id = rkk.id AND\n" +
            "                      rkkbase.id_type = rkk.id_type\n" +
            "WHERE rkkbase.id IN (SELECT Item FROM QR_Id_List WHERE Request = {0})\n" +
            "ORDER BY CASE WHEN {1} = 1 THEN rkk.regnumcnt END";

    private final CollectionsService collections = getBean("collectionsService");
    private final CrudService crud = getBean("crudService");
    private final IdConverter idConverter = getBean("idConverter");
    private final BarcodeStampGenerationService stampService = getBean("BarcodeStampGenerationService");

    @Nullable
    private IdentifiableObjectCollection findRequestsCollection(Value<?>... filterValue) {
        final List<Value<?>> filter = filterValue != null && filterValue.length > 0
                ? stream(filterValue).collect(toList())
                : emptyList();

        return collections.findCollectionByQuery(REQUEST_QUERY, filter);
    }

    @Nullable
    private String getUnid(@Nonnull Id id) {
        log.debug("getUnid(Object) / id={}", id);

        final DomainObject obj = crud.find(id);

        if (obj == null) {
            log.debug("getUnid(Object) / obj is null: return null");
            return null;
        }

        final String replicaId = idConverter.buildReplicaId(obj.getReference("module"));

        log.debug("getUnid(Object) / replicaId='{}'", replicaId);

        final String unid = idConverter.buildUnid(obj);

        log.debug("getUnid(Object) / unid='{}'", unid);

        return replicaId + ':' + unid;
    }

    @Nonnull
    private List<String> getDocUnids(@Nonnull Id requestId, boolean orderByRegNumber) {
        final IdentifiableObjectCollection collection = findRequestsCollection(new ReferenceValue(requestId), new BooleanValue(orderByRegNumber));

        return collection != null
                ? collection.stream().map(IdentifiableObject::getId).map(this::getUnid).collect(toList())
                : emptyList();
    }

    @Nullable
    private InputStream getBarcodeStamp(@Nonnull String docUnid,
                                        @Nonnull String fieldName,
                                        String additionalKey,
                                        boolean border,
                                        int ident) {
        try {
            final File stampFile = stampService.generate(docUnid, fieldName, additionalKey, border, ident);
            return new FileInputStream(stampFile);
        } catch (FileNotFoundException e) {
            log.error("getBarcodeStamp / file not found: return null", e);
            return null;
        } catch (Exception e) {
            log.error("getBarcodeStamp / generate error", e);
            return null;
        }
    }

    @Nonnull
    private JRDataSource getDataSource(@Nonnull Id requestId,
                                       boolean orderByRegNumber,
                                       @Nonnull String fieldName,
                                       String additionalKey,
                                       boolean border,
                                       int ident) {
        final List<ReportRow> rows = new ArrayList<>();
        final List<String> docUnids = getDocUnids(requestId, orderByRegNumber);
        for (final String unid : docUnids) {
            final InputStream image = getBarcodeStamp(unid, fieldName, additionalKey, border, ident);
            rows.add(new ReportRow(unid, image));
        }

        return new JRBeanCollectionDataSource(rows);
    }

    @Nullable
    private <T> T getMapValue(@Nonnull Map<String, Object> map, @Nonnull String key, @Nonnull Class<T> type, T defaultValue) {
        return ofNullable(map.get(key)).filter(type::isInstance).map(type::cast).orElse(defaultValue);
    }

    @Nullable
    private <T> T getMapValue(@Nonnull Map<String, Object> map, @Nonnull String key, @Nonnull Class<T> type) {
        return getMapValue(map, key, type, null);
    }

    @Override
    public JRDataSource getJRDataSource(Connection connection, Map<String, Object> map) throws Exception {
        final Id requestId = getMapValue(map, "request_id", Id.class);
        if (requestId == null) {
            throw new IllegalArgumentException("Parameter \"request_id\" cant be null.");
        }

        final boolean orderByRegNumber = getMapValue(map, "order_by_regnumber", Boolean.class, false);
        final String barcodeFieldName = getMapValue(map, "barcode_field_name", String.class, "barcode");
        final String stampAdditionalKey = getMapValue(map, "stamp_additional_key", String.class, "barcode-extended");
        final boolean stampBorder = getMapValue(map, "stamp_border", Boolean.class, false);
        final int stampIdent = getMapValue(map, "stamp_ident", Integer.class, 2);

        final boolean sessionAlreadyOpened = isDefinedOrOpened();
        if (!sessionAlreadyOpened) {
            defineLocalUser();
        }

        try {
            final String replicaIdSO = getMapValue(map, "replicaIdSO", String.class);
            if (isNotBlank(replicaIdSO)) {
                setCurrentOrganization(replicaIdSO);
            } else {
                log.warn("Parameter \"soReplicaId\" is blank: {}", replicaIdSO);
            }

            return getDataSource(requestId, orderByRegNumber, barcodeFieldName, stampAdditionalKey, stampBorder, stampIdent);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            if (!sessionAlreadyOpened) {
                close();
            }
        }
    }

    public static class ReportRow {
        private final String unid;
        private final InputStream image;

        public ReportRow(String unid, InputStream image) {
            this.unid = unid;
            this.image = image;
        }

        public String getUnid() {
            return unid;
        }

        public InputStream getImage() {
            return image;
        }
    }
}
