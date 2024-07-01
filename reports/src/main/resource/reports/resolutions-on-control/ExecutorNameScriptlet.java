import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Objects;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;


public class ExecutorNameScriptlet extends JRDefaultScriptlet {

    private String executorName = "";
    private final String groupType = "SO_OrgSystem";
    private final String departmentType = "SO_Department";
    private final String superiorType = "SO_AppointmentHead";
    private final String employeeType = "SO_AppointmentPlain";
    private final String NON_SYS_ORG_TYPE = "SO_OrgDescriptionNonsys";
    private final String roleType = "SO_Role";
    private final String beardType = "SO_Beard";

    private String query = "";


    public PreparedStatement PrepareStatement(Connection connection, Object idObject) throws Exception {
        RdbmsId id = (RdbmsId) idObject;
        String type = "";

        query = "SELECT name FROM domain_object_type_id WHERE id = " + Integer.toString(id.getTypeId());
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            type = resultSet.getString("name");
        }
        else {
            Exception ex = new Exception("Custom Exception: Unable to find object type in table domain_object_type_id - id " + Integer.toString(id.getTypeId()));
            throw ex;
        }
        statement.close();

        if(type.equalsIgnoreCase(superiorType) || type.equalsIgnoreCase(employeeType)) {
            query = "SELECT SO_Beard.orig_shortname as author, coalesce(SO_Beard.id,0) as beardId " +
                    "FROM SO_Appointment " +
                    "JOIN SO_Beard ON SO_Beard.id = SO_Appointment.beard " +
                    "WHERE SO_Appointment.id ";
        }
        else if(type.equalsIgnoreCase(groupType) || type.equalsIgnoreCase(departmentType))
        {
            query = "SELECT SO_Beard.orig_shortname as author, coalesce(SO_Beard.id,0) as beardId " +
                    "FROM so_structureunit " +
                    "JOIN so_beard ON so_structureunit.beard = so_beard.id " +
                    "WHERE so_structureunit.id ";
        }
        else if(type.equalsIgnoreCase(roleType))
        {
            query = "SELECT so_beard.orig_shortname as author, coalesce(SO_Beard.id,0) as beardId " +
                    "FROM so_role " +
                    "JOIN so_beard ON so_role.beard = so_beard.id " +
                    "WHERE so_role.id ";
        }
        else if (NON_SYS_ORG_TYPE.equalsIgnoreCase(type)){
            query = "SELECT \n" +
                    "  b.orig_shortname  AS author, \n" +
                    "  coalesce(b.id, 0) AS beardId \n" +
                    "FROM so_orgdescriptionnonsys org_non_sys \n" +
                    "  JOIN so_beard b ON org_non_sys.beard = b.id \n" +
                    "WHERE org_non_sys.id ";
        }
        else if (beardType.equalsIgnoreCase(type)) {
            query = "SELECT \n" +
                    "  b.orig_shortname  AS author, \n" +
                    "  b.id AS beardId \n" +
                    "FROM so_beard b \n" +
                    "WHERE b.id ";
        }
        else {
            Exception ex = new Exception("Report scriptlet error: Unexpected type of parameter encountered - " + type);
            throw ex;
        }
        String endQuery = " IN (";
        endQuery += id.getId();
        endQuery += ")";

        statement = connection.prepareStatement(query + endQuery);

        return statement;
    }

    public String getExecutorName(Connection connection, Object idObject) throws Exception {
        PreparedStatement statement = PrepareStatement(connection, idObject);
        ResultSet resultSet = statement.executeQuery();
        String executorName ="";
        while (resultSet.next()) {
            executorName = resultSet.getString("author");
        }
        statement.close();

        return executorName;
    }

    public long getExecutorId(Connection connection, Object idObject) throws Exception {
        PreparedStatement statement = PrepareStatement(connection, idObject);
        ResultSet resultSet = statement.executeQuery();
        long executorId = 0L;
        while (resultSet.next()) {
            executorId = resultSet.getLong("beardId");
        }
        statement.close();

        return executorId;
    }
}
