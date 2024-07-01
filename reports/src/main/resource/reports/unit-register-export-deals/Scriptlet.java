import net.sf.jasperreports.engine.JRDefaultScriptlet;

import ru.intertrust.cmj.af.core.AFSession;

public class Scriptlet extends JRDefaultScriptlet {

    /**
     * Получение ФИО текущего пользователя
     */
    public String getCurUserName() {
        return AFSession.get().currentUser().getBeard().originalData().getShortName();
    }
}