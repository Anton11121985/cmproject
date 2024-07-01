import java.util.List;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cmj.af.core.AFSession;
import ru.intertrust.cmj.af.so.SOApplication;
import ru.intertrust.cmj.af.so.SOBeard;
import ru.intertrust.cmj.af.so.SOParty;

import static java.util.Collections.singletonList;

public class ScriptletSo extends JRDefaultScriptlet {

    public String getIsolatedDep(String beardUnid) {
        if (!beardUnid.isEmpty()) {
            SOBeard beard = findBeardByUnid(beardUnid);
            if (beard != null) {
                SOBeard isolatedDep = beard.getIsolatedDep();
                if (isolatedDep != null) {
                    return isolatedDep.originalData().getShortName();
                } else {
                    return beard.getParent().originalData().getShortName();
                }
            }
        }
        return "";
    }

    private SOBeard findBeardByUnid(final String unid) {
        if (!AFSession.isDefinedOrOpened())
            AFSession.Manual.defineLocalUser();

        final SOApplication soApp = AFSession.get().getApplication(SOApplication.class);
        final List<SOBeard> beards = soApp.getBeardsByCMIds(singletonList(unid));

        SOBeard beard = beards != null && !beards.isEmpty() ? beards.get(0) : null;

        if (beard != null) {

            final SOBeard.DocflowData effectiveData = beard.effectiveData();
            if (effectiveData != null) {
                final SOParty party = effectiveData.getParty();
                if (party != null) {
                    final SOBeard effectiveBeard = party.getBeard();
                    if (effectiveBeard != null) {
                        return effectiveBeard;
                    }
                }
            }
        }
        AFSession.Manual.close();
        return beard;
    }
}